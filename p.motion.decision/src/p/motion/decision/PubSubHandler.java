package p.motion.decision;

import redis.clients.jedis.JedisPubSub;

public class PubSubHandler extends JedisPubSub {
	@Override
	public void onMessage(String channel, String message) {
//		System.out.println(channel);
//		System.out.println(message);
		if (message.startsWith("LINE")) {
			String[] arr = message.split(",");
//			System.out.println(message);
			long ts = Long.parseLong(arr[1]);
			int length = Integer.parseInt(arr[2]);
			double angle = Double.parseDouble(arr[3]);
			Decision.addLine(ts, length, angle);
		}
	}
}