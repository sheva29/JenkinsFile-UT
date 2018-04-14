import org.junit.Test

class JenkinsFilePlatoTest extends PipelineTest {

    @Test
    void "test sh calls gradlew build"() throws Exception {
        def expectedResults = new ArrayList<ArrayList<String>>(){{
            add(new ArrayList<String>(Arrays.asList("Cleanup workspace before build")))
            add(new ArrayList<String>(Arrays.asList("Checkout the code")))
            add(new ArrayList<String>(Arrays.asList("Unit tests", "Push Ansible to S3", "Start instances")))
            add(new ArrayList<String>(Arrays.asList("Build and push docker images for App and Properties", "Integration tests")))
            add(new ArrayList<String>(Arrays.asList("Check that instances become healthy")))
            add(new ArrayList<String>(Arrays.asList("Functional tests")))
        }}

        def jenkinsCommands = helper.callStack
        def results = new ArrayList<ArrayList<String>>()

        jenkinsCommands.each { jenkinsCommand ->
            if (jenkinsCommand.getMethodName().contains("stage")) {
                results.add(new ArrayList<String>(Arrays.asList(jenkinsCommand.args[0]) as Collection<? extends String>))
            }
            if (jenkinsCommand.getMethodName().contains("parallel")) {
                def parallelCommands = new ArrayList<String>()
                LinkedHashMap<String, String> parallel = jenkinsCommand.args[0] as LinkedHashMap<String, String>
                parallel.keySet().each { String key ->
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
