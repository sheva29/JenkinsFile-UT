import com.lesfurets.jenkins.unit.BasePipelineTest
import org.codehaus.groovy.runtime.GStringImpl
import org.junit.Before
import org.junit.Test

import java.util.stream.Collectors

class JenkinsFilePlatoTest extends BasePipelineTest {

    ArrayList<ArrayList<String>> expectedResults = new ArrayList<ArrayList<String>>(){{

        add(new ArrayList<String>(Arrays.asList("Cleanup workspace before build")))
        add(new HashSet<String>(Arrays.asList("Checkout the code")))
        add(new HashSet<String>(Arrays.asList("Unit tests", "Push Ansible to S3", "Start instances")))
        add(new HashSet<String>(Arrays.asList("Build and push docker images for App and Properties", "Integration tests")))
        add(new HashSet<String>(Arrays.asList("Check that instances become healthy")))
        add(new HashSet<String>(Arrays.asList("Functional tests")))
    }};

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        helper.registerAllowedMethod("timestamps", [Closure.class], null)
        helper.registerAllowedMethod("ansiColor", [String.class, Closure.class], null)
        helper.registerAllowedMethod("lock", [LinkedHashMap.class, Closure.class], null)
        helper.registerAllowedMethod("deleteDir", [], null)
        helper.registerAllowedMethod("timeout", [Integer.class, Closure.class], null)
        Checkout checkout = new Checkout()
        helper.registerAllowedMethod("checkout", [String.class], {checkout})

        Env env = new Env();
        binding.setVariable('env', env)

        Docker docker = new Docker();
        binding.setVariable('docker', docker)
        binding.setVariable('scm', "git@github.com:jenkinsci/JenkinsPipelineUnit.git")
    }

    class Env {
        private String BRANCH_NAME = "develop"
        private String EXECUTOR_NUMBER = "0"
    }

    class Checkout {
        private String GIT_COMMIT = "8f1a57068561f09a6f76db278ef274e02c6accc9"
    }

    class Docker {
        void build(Object image, String param){}
        Image image(GStringImpl imageName){return new Image()}
        class Image{
            void inside(GStringImpl string, Closure closure){}
            void inside(Closure closure){}
        }
    }

    @Test
    void "test sh calls gradlew build"() throws Exception {
        loadScript("JenkinsFile-Plato.groovy")
        printCallStack()

        def jenkinsCommands = helper.callStack

        ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>()

        //build state commands
        for (jenkinsCommand in jenkinsCommands) {
            if (jenkinsCommand.getMethodName().contains("stage")) {
                results.add(new ArrayList<String>(Arrays.asList(jenkinsCommand.args[0])))
            }
            if (jenkinsCommand.getMethodName().contains("parallel")) {
                ArrayList<String> parallelCommands = new ArrayList<String>()
                for (String key : jenkinsCommand.args[0].keySet()){
                    parallelCommands.add(key)
                }
                results.add(parallelCommands)
            }
        }
        assert expectedResults == results
    }


    @Test
    void "test content of parallel blocks"() throws Exception {

    }

    @Test
    void "test variables in commands"() throws Exception {

    }
}
