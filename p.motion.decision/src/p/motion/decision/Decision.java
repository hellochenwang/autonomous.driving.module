package p.motion.decision;

import java.util.LinkedList;

import p.motion.decision.PubSubHandler;
import redis.clients.jedis.Jedis;

public class Decision {

	public final static String KITTYBOT_URI = "192.168.43.206";
	public final static String VISION_UPDATE = "VISION";
	public final static String MOTION_UPDATE = "MOTION";
	public final static int CAM_INIT_ANGLE = 62;

	public static int w = 320;
	public static int h = 240;

	static void setCamTilt(int angle) {
		JedisQuery.mset("TILT", String.valueOf(angle));
		JedisQuery.publish(MOTION_UPDATE, "UPDATE");
	}

	static void shutdownWheels() {
		JedisQuery.mset("LEFT_ON", "0", "RIGHT_ON", "0");
		JedisQuery.publish(MOTION_UPDATE, "UPDATE");
	}

	static void updateSpeed() {
		JedisQuery.mset("LEFT", String.valueOf(leftSpeed), "RIGHT",
				String.valueOf(rightSpeed), "LEFT_ON", "1", "RIGHT_ON", "1");
		JedisQuery.publish(MOTION_UPDATE, "UPDATE");
	}

	private static Jedis JedisSub;
	private static Jedis JedisQuery;
	static int leftSpeed = 0;
	static int rightSpeed = 0;

	static LinkedList<TLA> lines = new LinkedList<TLA>();
	static LinkedList<TLA> vLines = new LinkedList<TLA>();
	static LinkedList<TLA> hLines = new LinkedList<TLA>();
	static TLA vLine;
	static TLA hLine;

	public static void addLine(long ts, int length, double angle) {
		TLA tla = new TLA();
		tla.ts = ts;
		tla.length = length;
		tla.angle = angle;
		lines.add(tla);
	}

	static void separateHVLines() {
		int lineSize = lines.size();

		// long currentTs = System.currentTimeMillis();
		vLines.clear();
		hLines.clear();
		for (int i = 0; i < lineSize; i++) {
			TLA line = lines.pollLast();
			// if (currentTs - line.ts > 2000) {
			// System.out.println("line is too old, break");
			// System.out.println(currentTs);
			// System.out.println(line.ts);
			// break;
			// }

			// instead of doing calculations on the fly, this part could be
			// pre-calculated.
			// i.e. if line.angle is in a range, then it's a vertical line, if
			// it's in another range, then it's a horizontal line.
			double a = Math.cos(line.angle);
			double b = Math.sin(line.angle);

			if (!Double.isNaN(a)) {
				int topX = (int) (line.length / a);
				int bottomX = (int) ((line.length - Decision.h * b) / a);
				if (topX >= 0 && topX <= Decision.w && bottomX >= 0
						&& bottomX <= Decision.w) {
					line.top = topX;
					line.bottom = bottomX;
					vLines.add(line);
				}
			}

			if (!Double.isNaN(b)) {
				int leftY = (int) (line.length / b);
				int rightY = (int) ((line.length - Decision.w * a) / b);
				if (leftY >= 0 && leftY <= Decision.h && rightY >= 0
						&& rightY <= Decision.h) {
					line.left = leftY;
					line.right = rightY;
					hLines.add(line);
				}
			}
		}
	}

	static void mergeVLines() {
		// amongst all the vertical lines, use one that is closest to the bottom
		// center.
		int minBottom = Integer.MAX_VALUE;
		TLA bestLine = null;
		for (TLA line : vLines) {
			int bottom = Math.abs(w / 2 - line.bottom);
			if (bottom < minBottom) {
				bestLine = line;
				minBottom = bottom;
			}
		}
		vLine = bestLine;
	}

	static void mergeHLines() {
		// amongst all the horizontal lines, use one whose mid-point is closest
		// to the bottom
		int minBottom = Integer.MAX_VALUE;
		TLA bestLine = null;
		for (TLA line : hLines) {
			int bottom = Math.abs(line.left - line.right) / 2
					+ (line.left > line.right ? line.right : line.left);
			if (bottom < minBottom) {
				bestLine = line;
				minBottom = bottom;
			}
		}
		hLine = bestLine;
	}

	static void decideVSpeed() {
		// if there's a vertical line in the center, then go straight.
		// if the vertical line has angle, turn the wheels accordingly.
		int leftSpeedFactor = 0;
		int rightSpeedFactor = 0;
		if (vLine != null) {
			int top = w / 2 - vLine.top;
			int bottom = w / 2 - vLine.bottom;

			int angle = (int) Math.toDegrees(vLine.angle);

			System.out.println("vLine.angle: " + angle);

			if (angle > 100) {
				if (top > 0) {
					if (angle < 176) {
						// Should turn left
						leftSpeedFactor = -1;
					}
				}
			} else { // angle < 100
				if (top < 0) {
					if (angle > 4) {
						// Should turn right
						rightSpeedFactor = -1;
					}
				}

			}

			// if (top > 0) {
			// // left should turn slower, but not a lot
			// leftSpeedFactor = (double) (160 - top) / (160 * 2);
			// } else if (top < 0) {
			// // right should turn slower, but not a lot
			// rightSpeedFactor = (double) (160 + top) / (160 * 2);
			// } else {
			// // go straight
			// }

			// if (bottom > 0) {
			// // left should turn slower, but not a lot
			// leftSpeedFactor = (double) (160 - bottom) / 160;
			// } else if (bottom < 0) {
			// // right should turn slower, but not a lot
			// rightSpeedFactor = (double) (160 + bottom) / 160;
			// } else {
			// // go straight
			// }

			leftSpeed = 123 + leftSpeedFactor;
			rightSpeed = 123 + rightSpeedFactor;

			// System.out.println("Left speed factor: " + leftSpeedFactor);
			// System.out.println("Right speed factor: " + rightSpeedFactor);
			//
			// System.out.println("Left speed: " + Decision.leftSpeed);
			// System.out.println("Right speed: " + Decision.rightSpeed);

		}
	}

	public static void main(String[] args) {
		JedisQuery = new Jedis(KITTYBOT_URI);

		Thread t = new Thread(new DecisionLoop());
		t.start();

		JedisSub = new Jedis(KITTYBOT_URI);
		JedisSub.subscribe(new PubSubHandler(), VISION_UPDATE);
	}
}
