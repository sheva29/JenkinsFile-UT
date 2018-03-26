import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

class JenkinsFileTest extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        // Assigns false to a job parameter ENABLE_TEST_STAGE
//        binding.setVariable('ENABLE_TEST_STAGE', 'false')
        // Defines the previous execution status
//        binding.getVariable('currentBuild').previousBuild = [result: 'UNSTABLE']

    }

    @Test
    void "test sh calls gradlew build"() throws Exception {
        def script = loadScript("JenkinsFile.jenkins")
        script.execute()
        printCallStack()

        assert helper.callStack.get(4).argsToString() == "./gradlew clean build"
    }

    @Test
    void "test order of steps in jenkins file"() throws Exception {
        def script = loadScript("JenkinsFile.jenkins")
        script.execute()
        printCallStack()

        def jenkinsCommands = helper.callStack

        def buildIndex = 0
        def testIndex = 0
        def i = 0
        for (jenkinsCommand in jenkinsCommands) {
            if (jenkinsCommand.argsToString() == "./gradlew clean build") {
                buildIndex = i
            }
            if (jenkinsCommand.argsToString() == "./gradlew test") {
                testIndex = i
            }
            i++
        }
        assert buildIndex != 0
        assert testIndex != 0
        assert buildIndex < testIndex
    }

}
