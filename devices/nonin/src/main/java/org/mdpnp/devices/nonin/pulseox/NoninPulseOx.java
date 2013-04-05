package org.mdpnp.devices.nonin.pulseox;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class NoninPulseOx {
	
	private final InputStream in;
	private final OutputStream out;
	
	public NoninPulseOx(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
		
		nextArrival = new Arrival();
		Arrival current = null, previous = nextArrival;
		for (int i = 1; i < SAMPLE_SIZE; i++) {
			current = new Arrival();
			previous.setNext(current);
			previous = current;
		}
		current.setNext(nextArrival);
	}
	

	
	public Boolean isArtifact() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getCurrentStatus().isArtifact();
	}

	public Boolean isRedPerfusion() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getCurrentStatus().isRedPerfusion();
	}

	public Boolean isGreenPerfusion() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getCurrentStatus().isGreenPerfusion();
	}

	public Boolean isYellowPerfusion() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getCurrentStatus().isYellowPerfusion();
	}

	public Integer getAvgHeartRateFourBeat() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgHeartRateFourBeat();
	}

	public Short getFirmwareRevision() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getFirmwareRevision();
	}

	public Integer getTimer() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getTimer();
	}

	public Boolean isSmartPoint() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.isSmartPoint();
	}

	public Boolean isLowBattery() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.isLowBattery();
	}

	public Short getAvgSpO2FourBeat() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgSpO2FourBeat();
	}

	public Short getAvgSpO2FourBeatFast() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgSpO2FourBeatFast();
	}

	public Short getSpO2BeatToBeat() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getSpO2BeatToBeat();
	}

	public Integer getAvgHeartRateEightBeat() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgHeartRateEightBeat();
	}

	public Short getAvgSpO2EightBeat() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgSpO2EightBeat();
	}

	public Short getAvgSpO2EightBeatForDisplay() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgSpO2EightBeatForDisplay();
	}

	public Integer getAvgHeartRateFourBeatForDisplay() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgHeartRateFourBeatForDisplay();
	}

	public Integer getAvgHeartRateEightBeatForDisplay() {
		Packet packet = getCurrentPacket();
		return null == packet ? null : packet.getAvgHeartRateEightBeatForDisplay();
	}
	
	public static final double MILLISECONDS_PER_SAMPLE = 1000.0 / (3.0 * Packet.FRAMES);

	public Double getMillisecondsPerSample() {
		return MILLISECONDS_PER_SAMPLE;
	}

	private final Packet currentPacket = new Packet();

	private Arrival nextArrival;
	private static final int SAMPLE_SIZE = 30;

	private double packetsPerSecond;
	private final Status status = new Status();
	private final Date date = new Date();
	public Date getTimestamp() {
		date.setTime(currentPacket.getFrameTime());
		return date;
	}
	
	protected static final byte OPCODE_SETFORMAT = 0x70;
	protected static final byte OPCODE_GETSERIAL = 0x74;
	protected static final byte OPCODE_RECVSERIAL = (byte) 0xF4;
	
	protected void sendOperation(byte opCode, byte[] data) throws IOException {
		sendOperation(opCode, data, 0, data.length);
	}
	
	protected void sendOperation(byte opCode, byte[] data, int off, int len) throws IOException {
		byte[] buf = new byte[4 + len];
		buf[0] = 0x02;
		buf[1] = opCode;
		buf[2] = (byte) len;
		System.arraycopy(data, off, buf, 3, len);
		buf[3+len] = 0x03;
		
		out.write(buf);
		out.flush();
	}
	
	protected void sendGetSerial() throws IOException {
		sendGetSerial(2);
	}
	
	protected  void sendGetSerial(int id) throws IOException {
		sendOperation(OPCODE_GETSERIAL, new byte[] { (byte)id, (byte)id});
	}
	
	protected void sendSetFormat(int format, boolean spotCheckMode, boolean bluetoothEnabledAtPowerOn) throws IOException {
		byte[] msg = new byte[] { 0x02, (byte)format, 0x01, (byte)(OPCODE_SETFORMAT + 4 + 2 + format) };
		msg[2] |= spotCheckMode ? 0x40 : 0x00;
		msg[2] |= bluetoothEnabledAtPowerOn ? 0x20 : 0x00;
		msg[3] += msg[2];
		sendOperation(OPCODE_SETFORMAT, msg);
	}
	
	protected  void sendSetFormat(int format) throws IOException {
		byte[] msg = new byte[] { 0x02, (byte)format };
		sendOperation(OPCODE_SETFORMAT, msg);
	}
	
	
	private static class Operation {
		byte opCode;
		byte[] msg;
	}
	
	private Boolean ackFlag;
	private Operation operationFlag;
	public boolean readyFlag = false;
	
	public synchronized String fetchSerial() throws IOException {
		String guid;
		this.operationFlag = null;
		long start = System.currentTimeMillis();
		
		while( (operationFlag == null) || (operationFlag.opCode != OPCODE_RECVSERIAL)) {
			this.operationFlag = null;
			if( (System.currentTimeMillis()-start) >= 10000L) {
				return null;
			}
			sendGetSerial();
			try {
				this.wait(2000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		guid = new String(operationFlag.msg, 1, 9);
		this.operationFlag = null;
		this.notifyAll();
		return guid;
	}
	
	interface Formatter {
		void sendSetFormat() throws IOException;
	}
	
	public synchronized boolean setDataFormat(Formatter formatter) throws IOException {
		Boolean ack;
		
		this.ackFlag = null;
		while(null == ackFlag) {
			formatter.sendSetFormat();
			try {
				this.wait(5000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		ack = ackFlag;
		this.ackFlag = null;
		this.notifyAll();
		return ack;
	}

	public final Formatter onyxFormat = new Formatter() {
		@Override
		public void sendSetFormat() throws IOException {
			NoninPulseOx.this.sendSetFormat(0x07);
		}
	};
	public final Formatter wristOxFormat = new Formatter() {
		public void sendSetFormat() throws IOException {
			NoninPulseOx.this.sendSetFormat(0x07, true, true);
		};
	};
	private static class Arrival {
		private Arrival next;
		private long tm;

		public void setNext(Arrival next) {
			this.next = next;
		}

		public void setTm(long tm) {
			this.tm = tm;
		}

		public Arrival getNext() {
			return next;
		}

		public long getTm() {
			return tm;
		}
	}
	
	protected synchronized void recvAcknowledged(boolean success) {
		ackFlag = success;
		this.notifyAll();
	}
	
	protected synchronized void recvOperation(byte opCode, byte[] source, int off, int len) {
//		System.out.println("Operation " + Integer.toHexString(0xFF&opCode) + " " + Util.bytesString(source, off, len));
		Operation op = new Operation();
		op.opCode = opCode;
		op.msg = new byte[len];
		System.arraycopy(source, off, op.msg, 0, len);
		this.operationFlag = op;
		this.notifyAll();
		
	}
	
	protected void frameError(String msg) {
		if(readyFlag) {
			System.err.println("frameError:"+msg);
		}
	}

	private final boolean consumeControl(byte[] buffer, int[] len) throws IOException {
		// Special control characters
		switch(buffer[0]) {
		case 0x02:
			// we don't yet know the size
			if(len[0] < 3) {
				return true;
			}
			int oplen = buffer[2] + 4;
			// we know the size and need more bytes
			if(len[0] < oplen) {
				return true;
			}
			// receive the operation
			recvOperation(buffer[1], buffer, 3, buffer[2]);
			
			System.arraycopy(buffer, oplen, buffer, 0, len[0] - oplen);
			len[0] -= oplen;
			return false;
		case 0x06:
			recvAcknowledged(true);
			System.arraycopy(buffer, 1, buffer, 0, len[0] - 1);
			len[0]--;
			return false;
		case 0x15:
			System.arraycopy(buffer, 1, buffer, 0, len[0] - 1);
			len[0]--;
			recvAcknowledged(false);
			return false;
		default:
			return false;
		}
	}
	
	
	public double getPacketsPerSecond() {
		return packetsPerSecond;
	}

	public Integer getHeartRate() {
		Boolean sensorDetached = isSensorAlarm();
		if(null == sensorDetached) {
			return null; 
		} else {
			return sensorDetached ? null : currentPacket.getAvgHeartRateFourBeat();
		}
	}

	public Integer getSpO2() {
		Boolean sensorDetached = isSensorAlarm();
		if(null == sensorDetached) {
			return null;
		} else {
			return sensorDetached ? null : (int) currentPacket.getAvgSpO2FourBeat();
		}
	}

	public Boolean isOutOfTrack() {
		Packet packet = currentPacket;
		return null == packet ? null : packet.getCurrentStatus().isOutOfTrack();
	}

	public Boolean isSensorAlarm() {
		Packet packet = currentPacket;
		return null == packet ? null : packet.getCurrentStatus().isSensorAlarm();
	}

	public Packet getCurrentPacket() {
		return currentPacket;
	}
	
	public void receivePacket(Packet packet) {
		
	}
	
	private final boolean consumeFrame(byte[] buffer, int[] len) throws IOException {
		if(len[0] < Packet.FRAME_LENGTH) {
			return true;
		}

		if (expectNewPacket && !status.set(buffer[0]).isSync()) {
			frameError("RESYNC");
			System.arraycopy(buffer, Packet.FRAME_LENGTH, buffer, 0, len[0] - Packet.FRAME_LENGTH);
			len[0] -= Packet.FRAME_LENGTH;
			return false;
		}

		
		if(readyFlag) {
			if(Packet.validChecksum(buffer, 0)) {
		
				boolean packetComplete = currentPacket.setFrame(buffer, 0);
		
				if(packetComplete) {
					expectNewPacket = true;
					
					receivePacket(currentPacket);
					
					
					
					long now = System.currentTimeMillis();
		
					nextArrival.setTm(now);
		
					Arrival next = nextArrival.getNext();
					long nextTime = next.getTm();
					if (0L != nextTime) {
						double elapsed = (now - nextTime) / 1000.0;
						packetsPerSecond = 1.0 * SAMPLE_SIZE / elapsed;
					} else {
						packetsPerSecond = 0;
					}
		
					nextArrival = nextArrival.getNext();
				} else {
					expectNewPacket = false;
				}
			} else {
				frameError("invalid checksum");
			}
		}
		
		len[0] -= Packet.FRAME_LENGTH;
		System.arraycopy(buffer, Packet.FRAME_LENGTH, buffer, 0, len[0]);
		
		return false;

	}
	
	private final void consume(byte[] buffer, int[] len) throws IOException {
		while(len[0] > 0) {
			switch(buffer[0]) { 
			case 0x02:
			case 0x06:
			case 0x15:
				// return true to indicate more data are needed!
				if(consumeControl(buffer, len)) {
					return;
				}
				break;
			default:
				if(consumeFrame(buffer, len)) {
					return;
				}
				break;
			}
		}
	}
	
	private boolean expectNewPacket = false;
	
	private byte[] buffer = new byte[Packet.LENGTH*3];
	private int b;
	private int[] len = new int[] {0};
	
	protected boolean receive() throws IOException {
		b = in.read(buffer, len[0], buffer.length - len[0]);
		
		// Read EOF, we're done
		if(b < 0) {
			readyFlag = false;
			return false;
		} else {
			len[0] += b;
		}
		
		consume(buffer, len);
		return true;
	}
}
