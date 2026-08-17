import com.Hecate.puppet.config.PuppetConfig;
import com.Hecate.puppet.config.PuppetIO;
import java.io.File;

public class TestPuppetLoad {
    public static void main(String[] args) {


        String testFile = "EXAMPLE_PUPPET_JSON.json";
        File file = new File(testFile);


        if (!file.exists()) {

            return;
        }

        try {

            PuppetConfig config = PuppetIO.loadFromFile(testFile);


            if (config.getBones().size() > 0) {

                var bone = config.getBones().get(0);

                if (bone.getPartConfig() != null) {

                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
