import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import com.example.bbsanimtweaker.client.logic.AnimTweakerEngine;

public class TestExtract3 {
    public static void main(String[] args) throws Exception {
        File targetFile = new File("C:/Users/saimd/Downloads/betterparticle/model.bbs.json");
        if (!targetFile.exists()) {
            System.out.println("Target file does not exist!");
            return;
        }

        System.out.println("Analyzing animation names in model.bbs.json:");
        List<String> animations = AnimTweakerEngine.getAnimationNames(targetFile);
        System.out.println("Found animations: " + animations);

        System.out.println("\nTesting query injection on target bone 'steeringwheel':");
        // We will try to inject Z-Yaw (+) to steeringwheel in "running" animation
        boolean success = AnimTweakerEngine.injectQueries(
            targetFile, 
            "steeringwheel", 
            Arrays.asList("running"), 
            null, // xQuery
            null, // yQuery
            "+query.head_yaw", // zQuery
            "" // mathModifier
        );
        System.out.println("Injection result: " + success);
    }
}
