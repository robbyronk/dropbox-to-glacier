package in.ayecapta.dbx2s3;

import in.ayecapta.photobackup.events.Startup;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class Main {
    public static void main(String[] args) {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        container.event().select(Startup.class).fire(new Startup());
        weld.shutdown();
    }
}
