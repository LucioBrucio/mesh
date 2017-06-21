// The GIT repository for this pipeline lib is defined in the global Jenkins setting
@Library('jenkins-pipeline-library')
import com.gentics.*

// Make the helpers aware of this jobs environment
JobContext.set(this)


properties([
	parameters([
		booleanParam(name: 'runTests',   defaultValue: true,  description: "Whether to run the unit tests"),
		booleanParam(name: 'runDeploy',  defaultValue: false, description: "Whether to run the deploy steps.")
	])
])

final def sshAgent             = "601b6ce9-37f7-439a-ac0b-8e368947d98d"
final def dockerHost           = "tcp://gemini.office:2375"
final def gitCommitTag         = '[Jenkins | ' + env.JOB_BASE_NAME + ']';


podTemplate(name: 'kubernetespod', label: 'kubernetespod',

	containers: [
		containerTemplate(name: 'jnlp', image: 'jenkinsci/jnlp-slave', args: '${computer.jnlpmac} ${computer.name}', workingDir: '/home/jenkins/workspace'),
		containerTemplate(name: 'genticsbuild', image: 'registry.cluster.gentics.com/jenkins-slave:kubernetes', alwaysPullImage: true, ttyEnabled: true, command: 'cat', workingDir: '/home/jenkins/workspace',
			envVars: [
				containerEnvVar(key: 'HOME', value: '/home/jenkins')
		]),
	],
	volumes: [
		//persistentVolumeClaim(claimName: 'jenkins-maven-repository', mountPath: '/home/jenkins/.m2/repository')
	]
) {
	node('kubernetespod') {
		stage("Checkout") {
			checkout([$class: 'GitSCM', branches: [[name: '*/kubernetes']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'LocalBranch']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git', url: 'git@github.com:gentics/mesh.git']]])
		}
		def branchName = GitHelper.fetchCurrentBranchName()

		stage("Set Version") {
			if (Boolean.valueOf(runReleaseBuild)) {
				version = MavenHelper.getVersion()
				if (version) {
					echo "Building version " + version
					version = MavenHelper.transformSnapshotToReleaseVersion(version)
					MavenHelper.setVersion(version)
				}
				//TODO only add pom.xml files
				sh 'git add .'
				sh "git commit -m 'Raise version'"
				GitHelper.addTag(version, 'Release of version ' + version)
			}
		}

		stage("Test") {
			if (Boolean.valueOf(runTests)) {
				def splits = 25;
				sh "find -name \"*Test.java\" | grep -v Abstract | shuf | sed  's/.*java\\/\\(.*\\)/\\1/' > alltests"
				sh "split -a 2 -d -n l/${splits} alltests  includes-"
				stash includes: '*', name: 'project'
				def branches = [:]
				for (int i = 0; i < splits; i++) {
					def current = i
					branches["split${i}"] = {
						container('genticsbuild') {
							echo "Preparing slave environment for ${current}"
							checkout([$class: 'GitSCM', branches: [[name: '*/kubernetes']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'LocalBranch']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git', url: 'git@github.com:gentics/mesh.git']]])
							unstash 'project'
							def postfix = current;
							if (current <= 9) {
								postfix = "0" + current 
							}
							echo "Setting correct inclusions file ${postfix}"
							sh "mv includes-${postfix} inclusions.txt"
							sshagent([sshAgent]) {
								try {
									sh "mvn -fae -Dmaven.test.failure.ignore=true -B -U -e -P inclusions -pl '!demo,!doc,!server,!performance-tests' clean test"
								} finally {
									step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
								}
							}
						}
					}
				}
				try {
					parallel branches
				} catch (err) {
					echo "Failed " + err.getMessage()
					error err.getMessage()
				}
			} else {
				echo "Tests skipped.."
			}
		}

		stage("Release Build") {
			if (Boolean.valueOf(runReleaseBuild)) {
				sshagent([sshAgent]) {
					sh "mvn -B -DskipTests clean package"
				}
			} else {
				echo "Release build skipped.."
			}
		}

		stage("Docker Build") {
			if (Boolean.valueOf(runDocker)) {
				withEnv(["DOCKER_HOST=" + dockerHost ]) {
					sh "rm demo/target/*sources.jar"
					sh "rm server/target/*sources.jar"
					sh "captain build"
				}
			} else {
				echo "Docker build skipped.."
			}
		}

		stage("Performance Tests") {
			if (Boolean.valueOf(runPerformanceTests)) {
				container('genticsbuild') {
					checkout([$class: 'GitSCM', branches: [[name: '*/kubernetes']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'LocalBranch']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git', url: 'git@github.com:gentics/mesh.git']]])
					try {
						sh "mvn -B -U clean package -pl '!doc,!demo,!verticles,!server' -Dskip.unit.tests=true -Dskip.performance.tests=false -Dmaven.test.failure.ignore=true"
					} finally {
						//step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
						step([$class: 'JUnitResultArchiver', testResults: '**/target/*.performance.xml'])
					}
				}
			} else {
				echo "Performance tests skipped.."
			}
		}

		stage("Integration Tests") {
			if (Boolean.valueOf(runIntegrationTests)) {
				withEnv(["DOCKER_HOST=" + dockerHost, "MESH_VERSION=" + version]) {
					sh "integration-tests/test.sh"
				}
			} else {
				echo "Performance tests skipped.."
			}
		}

		stage("Deploy") {
			if (Boolean.valueOf(runDeploy)) {
				if (Boolean.valueOf(runDocker)) {
					withEnv(["DOCKER_HOST=" + dockerHost]) {
						withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'dockerhub_login', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USERNAME']]) {
							sh 'docker login -u $DOCKER_HUB_USERNAME -p $DOCKER_HUB_PASSWORD -e entwicklung@genitcs.com'
							sh "captain push"
						}
					}
				}
				sshagent([sshAgent]) {
					sh "mvn -U -B -DskipTests clean deploy"
				}
			} else {
				echo "Deploy skipped.."
			}
		}

		stage("Git push") {
			if (Boolean.valueOf(runReleaseBuild)) {
				sshagent([sshAgent]) {
					def snapshotVersion = MavenHelper.getNextSnapShotVersion(version)
					MavenHelper.setVersion(snapshotVersion)
					GitHelper.addCommit('.', gitCommitTag + ' Prepare for the next development iteration (' + snapshotVersion + ')')
					GitHelper.pushBranch(branchName)
					GitHelper.pushTag(version)
				}
			} else {
				echo "Push skipped.."
			}
		}
	}
}
