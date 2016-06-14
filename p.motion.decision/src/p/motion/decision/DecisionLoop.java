package p.motion.decision;

public class DecisionLoop implements Runnable {

	/*
	 * Cam position: initial 62
	 * 
	 * Normal running speed: 120 - 127
	 */

	@Override
	public void run() {

		Decision.setCamTilt(Decision.CAM_INIT_ANGLE);

		for (;;) {
			Decision.separateHVLines();
			Decision.mergeVLines();
			Decision.mergeHLines();

			Decision.decideVSpeed();

			Decision.updateSpeed();

			if (Decision.lines.size() > 2000) {
				Decision.lines.clear();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}