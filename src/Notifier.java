import java.util.HashMap;


public interface Notifier {
	public boolean send(HashMap data);
	public void setSetting(HashMap setting) throws Exception;
}
