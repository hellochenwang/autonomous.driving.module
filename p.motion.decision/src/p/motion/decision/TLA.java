package p.motion.decision;

public class TLA {
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