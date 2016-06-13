package p.motion.decision;

import java.util.LinkedList;

import redis.clients.jedis.Jedis;

/*
 * Cam position:
 * 	initial 62
 * 
 * Normal running speed: 120 - 127
 * 
 * 
 */
public class Decision {

	final public static int CAM_INIT_ANGLE = 62;

	public static int w = 320;
	public static int h = 240;

	private static Runnable thread = new Runnable() {

		@Override
		public void run() {

			setCamTilt(CAM_INIT_ANGLE);

			for (;;) {
				separateHVLines();
				mergeVLines();
				// mergeHLines();

				leftSpeed = 120;
				rightSpeed = 127;

				System.out.println("Line size: " + lines.size());
				System.out.println("Left speed: " + leftSpeed);
				System.out.println("Right speed: " + rightSpeed);

				updateSpeed();

				if (lines.size() > 2000) {
					lines.clear();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				leftSpeed = 0;
				rightSpeed = 0;
				updateSpeed();

				shutdownWheels();
				break;
			}
		}
	};

	private static void setCamTilt(int angle) {
		JedisQuery.mset("TILT", String.valueOf(angle));
		JedisQuery.publish(MotionDecision.MOTION_UPDATE, "UPDATE");
	}

	private static void shutdownWheels() {
		JedisQuery.mset("LEFT_ON", "0", "RIGHT_ON", "0");
		JedisQuery.publish(MotionDecision.MOTION_UPDATE, "UPDATE");
	}

	private static void updateSpeed() {
		JedisQuery.mset("LEFT", String.valueOf(leftSpeed), "RIGHT",
				String.valueOf(rightSpeed), "LEFT_ON", "1", "RIGHT_ON", "1");
		JedisQuery.publish(MotionDecision.MOTION_UPDATE, "UPDATE");
	}

	private static Jedis JedisQuery;
	private static int leftSpeed = 0;
	private static int rightSpeed = 0;

	private static class TLA {
		long ts;
		int length;
		double angle;

		int top = -1;
		int bottom = -1;
		int left = -1;
		int right = -1;

		boolean isVertical() {
			if (top != -1 && bottom != -1 && left == -1 && right == -1) {
				return true;
			} else {
				return false;
			}
		}

		boolean isHorizontal() {
			if (top == -1 && bottom == -1 && left != -1 && right != -1) {
				return true;
			} else {
				return false;
			}
		}
		
		boolean alreadyMerged = false;
	}

	private static LinkedList<TLA> lines = new LinkedList<TLA>();
	private static LinkedList<TLA> vLines = new LinkedList<TLA>();
	private static LinkedList<TLA> hLines = new LinkedList<TLA>();
	private static TLA vLine = new TLA();
	private static TLA hLine = new TLA();

	public static void addLine(long ts, int length, double angle) {
		TLA tla = new TLA();
		tla.ts = ts;
		tla.length = length;
		tla.angle = angle;
		lines.add(tla);
	}

	public static int getLeftSpeed() {
		return leftSpeed;
	}

	public static int getRightSpeed() {
		return rightSpeed;
	}

	public static void startThread() {
		JedisQuery = new Jedis("192.168.43.206");

		Thread t = new Thread(thread);
		t.start();
	}

	private static void separateHVLines() {
		int lineSize = lines.size();
		long currentTs = System.currentTimeMillis();
		vLines.clear();
		hLines.clear();
		for (int i = 0; i < lineSize; i++) {
			TLA line = lines.pollLast();
			if (currentTs - line.ts > 2000) {
				continue;
			}

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

	private static void mergeVLines() {
		// if lines are close by(within xx pixels), they can be merged.
		// if there are more than 3 lines,
		// 160-132 = 28 px is the width of the black line
		TLA[] vLineArr = vLines.toArray(new TLA[vLines.size()]);
		for (int i = 0; i < vLineArr.length; i++) {
			TLA line1 = vLineArr[i];
			for (int j = i + 1; j < vLineArr.length; j++) {
				TLA line2 = vLineArr[j];
				int topDist = line1.top - line2.top;
				int bottomDist = line1.bottom - line2.bottom;
				if (Integer.signum(topDist) == Integer.signum(bottomDist)) {
					if (Math.abs(topDist) < 30 && Math.abs(bottomDist) < 30) {
						// Merge
						// New line is the middle point
						// New line will override line1 and no more merge. 
						break;
					} else {
						// Do nothing
					}
				}else{
					// Do nothing
				}
			}

		}

		for (TLA line1 : hLines) {
			double a = Math.cos(line1.angle);
			double b = Math.sin(line1.angle);

			int topX = (int) (line1.length / a);
			int bottomX = (int) ((line1.length - Decision.h * b) / a);
			if (bottomX >= 80 && bottomX <= 240) {
				if (topX >= 110 && topX <= 210) {
					// Go straight with minor adjustments
				}
			}
			if ((bottomX >= 0 && bottomX < 80)
					|| (bottomX > 240 && bottomX <= 320)) {
				if (topX >= 0 && topX <= 320) {
					// Go straight with major adjustments
				}
			}
		}
	}

	/**
	 * input: guaranteed lines output: angle and speed
	 */
	public static void decideSpeedAndAngle(TLA[] lines) {
		// if there's a vertical line in the center, then go straight.
		// if the vertical line has angle,
		for (TLA line : lines) {

		}
	}

	/**
	 * input: angle and speed output: motor commands
	 */
	public static void decideMotorCommand() {

	}
}
