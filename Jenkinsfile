pipeline {

  parameters { booleanParam(name: 'DEPLOY_ARTIFACT', defaultValue: false, description: 'Deploy Artifact') }

  agent {
    docker {
      image 'maven:3.6-openjdk-11'
      args '--network=host'
    }
  }

  stages {

    stage('Build') {
      steps {
        configFileProvider([configFile(fileId: 'MavenSettings', variable: 'MAVEN_SETTINGS_XML')]) {
          withMaven(){
            sh "mvn -s $MAVEN_SETTINGS_XML clean install"
          }
        }
      }
    }

    stage('Deploy Artifact') {
      when {
        expression { params.DEPLOY_ARTIFACT == true }
      }
      steps {
        configFileProvider([configFile(fileId: 'MavenSettings', variable: 'MAVEN_SETTINGS_XML')]) {
          withMaven(){
            sh "mvn -s $MAVEN_SETTINGS_XML deploy"
          }
        }
      }
    }
  }

}