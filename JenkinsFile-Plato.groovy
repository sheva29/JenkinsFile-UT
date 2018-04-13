timestamps {
    ansiColor('xterm') {
        node('ecmcore-cd.dev') {
            lock(resource: "lock_${env.BRANCH_NAME}", inversePrecedence: true) {
                stage('Cleanup workspace before build') {
                    deleteDir()
                }

                stage('Checkout the code') {
                    GIT_COMMIT=checkout(scm).GIT_COMMIT
                }

                def gradleCacheVolumeMount = " -v ${createGradleCacheVolumeIfNotExist()}:/gradle -e GRADLE_USER_HOME=/gradle"
                def javaAwscliDockerImage = "java-awscli-docker:${UUID.randomUUID()}"
                def dockerContainerOptions = buildDockerCapableContainer(javaAwscliDockerImage)

                parallel (
                        'Unit tests' : {
                            docker.image(javaAwscliDockerImage).inside (gradleCacheVolumeMount + dockerContainerOptions) {
                                sh './gradlew --stacktrace clean build test -q'
                            }
                        },

                        'Push Ansible to S3' : {
                            docker.image(javaAwscliDockerImage).inside() {
                                sh """ tar zcvf ./box-startup.tgz ./box-startup &&\
                                aws s3 cp ./box-startup.tgz s3://infrastructure-deploy-nyt-net/ecmcore/dev/container-demo/${GIT_COMMIT.substring(0, 7)}.tgz """
                            }
                        },

                        'Start instances' : {
                            withCredentials([string(credentialsId: 'Nimbul_API_Key_Ecmcore_Dev_Admins', variable: 'nimbul_api_token')]) {
                                docker.image(javaAwscliDockerImage).inside() {
                                    sh './pipeline/start_instances.bash'
                                }
                            }
                        }
                )

                parallel (
                        'Build and push docker images for App and Properties' : {
                            docker.image(javaAwscliDockerImage).inside (gradleCacheVolumeMount + dockerContainerOptions) {
                                sh './gradlew dockerPushAppAndProperties'
                            }
                        },

                        'Integration tests' : {
                            sleep 10
                        }
                )

                timeout(5) {
                    stage('Check that instances become healthy')  {
                        withCredentials([string(credentialsId: 'Nimbul_API_Key_Ecmcore_Dev_Admins', variable: 'nimbul_api_token')]) {
                            docker.image(javaAwscliDockerImage).inside() {
                                sh './pipeline/check_instances_become_ready.bash'
                            }
                        }
                    }
                }

                stage('Functional tests') {
                    sleep 10
                }
            }
        }
    }
} // ansiColor timestamps

String buildDockerCapableContainer(String imageId) {
    def userId = sh (
            script: "id --user",
            returnStdout: true
    )
    def dockerGroupId = sh (
            script: ''' getent group docker | awk -F: '{printf $3}' ''',
            returnStdout: true
    )

    String gidArg = "--build-arg GID=12345"
    if (dockerGroupId) {
        gidArg = "--build-arg GID=$dockerGroupId"
    }

    docker.build(imageId, "--file pipeline/docker/java-awscli-docker/Dockerfile --build-arg UID=$userId $gidArg .")

    return " -v /var/run/docker.sock:/var/run/docker.sock -u $userId "
}

def createGradleCacheVolumeIfNotExist() {
    String gradleCacheVolumeName = "gradle-cache-volume-${env.EXECUTOR_NUMBER}"
    try {
        sh "docker volume inspect $gradleCacheVolumeName"
    } catch (e) {
        sh "docker volume create --name $gradleCacheVolumeName"
    }

    return gradleCacheVolumeName
}