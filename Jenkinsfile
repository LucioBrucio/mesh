stage 'Test'
def splits = splitTests parallelism: [$class: 'CountDrivenParallelism', size: 15], generateInclusions: true
def branches = [:]
for (int i = 0; i < splits.size(); i++) {
  def split = splits[i]
  branches["split${i}"] = {
    node('!master') {
      checkout scm
      writeFile file: (split.includes ? 'inclusions.txt' : 'exclusions.txt'), text: split.list.join("\n")
      writeFile file: (split.includes ? 'exclusions.txt' : 'inclusions.txt'), text: ''
      sh "cat exclusions.txt"
      sh "cat inclusions.txt"
      sh "ls -la"
      def mvnHome = tool 'M3'
      sh "${mvnHome}/bin/mvn -B clean test -Dmaven.test.failure.ignore"
      step([$class: 'JUnitResultArchiver', testResults: 'target/surefire-reports/*.xml'])
    }
  }
}
parallel branches
  
