package video2;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Created by CP on 2017/1/9. 14:56.
 */
public interface IEvent {

    void connect(SelectionKey key) throws IOException;
    void read(SelectionKey key) throws IOException;
    void write() throws IOException;
    void error(Exception e);
}
