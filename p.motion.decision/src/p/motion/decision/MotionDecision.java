package p.motion.decision;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class MotionDecision {

	public final static String VISION_UPDATE = "VISION";
	public final static String MOTION_UPDATE = "MOTION";

	private static Jedis JedisSub;
	private static Jedis JedisQuery;

	static JedisPubSub sub = new JedisPubSub() {

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels) {
			System.out.println("onUnsubscribe " + channel);
		}

		@Override
		public void onSubscribe(String channel, int subscribedChannels) {
		}

		@Override
		public void onPUnsubscribe(String pattern, int subscribedChannels) {
		}

		@Override
		public void onPSubscribe(String pattern, int subscribedChannels) {
		}

		@Override
		public void onPMessage(String pattern, String channel, String message) {
			System.out.println("onPMessage: " + message);
			// broadcast to all web socket clients
		}

		@Override
		public void onMessage(String channel, String message) {
			System.out.println(channel);
			System.out.println(message);
			if(message.startsWith("LINE")){
				String[] arr = message.split(",");
				System.out.println(message);
				long ts = Long.parseLong(arr[1]);
				int length = Integer.parseInt(arr[2]);
				double angle = Double.parseDouble(arr[3]);
				Decision.addLine(ts, length, angle);
			}
		}
	};

	public static void start() {
		Decision.startThread();

		JedisQuery = new Jedis("192.168.43.206");
		JedisSub = new Jedis("192.168.43.206");
		JedisSub.subscribe(sub, VISION_UPDATE); // it will block here
	}

	public static void stop() {
		JedisSub.close();
		JedisQuery.close();
	}

	public static void publishToMotion(String message) {
		JedisQuery.publish(MOTION_UPDATE, message);
	}

	public static void set(String key, String value) {
		JedisQuery.set(key, value);
	}

	public static String get(String key) {
		return JedisQuery.get(key);
	}

	public static void main(String[] args) {
		MotionDecision.start();
	}

}
