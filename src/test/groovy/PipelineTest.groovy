import com.lesfurets.jenkins.unit.BasePipelineTest
import org.codehaus.groovy.runtime.GStringImpl
import org.junit.Before

class PipelineTest extends BasePipelineTest {

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

        loadScript("JenkinsFile-Plato.groovy")
        printCallStack()
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
        Image image(String imageName){return new Image()}
        class Image{
            String inside(String string, Closure closure){return string;}
            void inside(Closure closure){}
        }
    }
}
