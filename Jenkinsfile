pipeline {
  agent any
  stages {
    stage('Precheck') {
      steps {
        sh '''
				echo ORACLE_HOME=${ORACLE_HOME}
				echo PATH=${PATH}
				whoami
				which git
				#which expdp
				git --version
				git config --global user.email "jenkins@datical.com"
				git config --global user.name "jenkins"
			'''
      }
    }

    stage('Checkout') {
      steps {
        deleteDir()
        checkout([
                $class: 'GitSCM',
                branches: [[name: '*/master']],
                doGenerateSubmoduleConfigurations: false,
                extensions: [
                     [$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ_DDB}"],
                     [$class: 'LocalBranch', localBranch: 'master']],
                     submoduleCfg: [],
                     userRemoteConfigs: [[url: "${GITURL}/${GIT_DATICAL_REPO}.git"]]
                 ])
            checkout([
                 $class: 'GitSCM',
                 branches: [[name: "$BRANCH"]],
                 doGenerateSubmoduleConfigurations: false,
                 extensions: [
                 	[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ_SQL}"], 
                        [$class: 'LocalBranch', localBranch: "${BRANCH}"]],
                        submoduleCfg: [],
                        userRemoteConfigs: [[url: "${GITURL}/${GIT_SQL_REPO}.git"]]
                 ])
              }
            }

            stage('Branches') {
              steps {
                sh '''
          		#{ set +x; } 2>/dev/null
 
          		cd ${PROJ_DDB}
          		echo "Current Directory:" `pwd`
          		git branch --set-upstream-to=origin/master master
          		git status
 
          		cd ../${PROJ_SQL}
          		echo "Current Directory: " `pwd`
            		git branch --set-upstream-to=origin/$BRANCH $BRANCH
 
          		git status 
        	'''
              }
            }

            stage('Packager') {
              steps {
                sh '''
				{ set +x; } 2>/dev/null
											  
				cd ${PROJ_DDB}
				PATH=/home/delphix_os/DaticalDB/repl:${PATH}
				echo
				echo "==== Running - hammer version ===="
				hammer show version
			 
				# invoke Datical DB\'s Deployment Packager
				echo "==== Running Deployment Packager ===="

				hammer groovy deployPackager.groovy pipeline=${DATICAL_PIPELINE} scm=true labels="${DATICAL_PIPELINE}"
				rc=$?
				if [[ ${rc} -ne 0 ]] 
				then
				    echo "err logic goes here ..."
				else 
				    echo "packager approved, deploy code ..."
				fi

	   		'''
              }
           }
           stage('Deployment') {
              steps {
                sh '''
				{ set +x; } 2>/dev/null
											  
				cd ${PROJ_DDB}
				PATH=/home/delphix_os/DaticalDB/repl:${PATH}
				echo
				echo "==== Running - hammer deploy ===="

			        hammer deploy dev --labels="${DATICAL_PIPELINE}"

	   		'''
              }
           }
	  
          }
          environment {
            GITURL = 'git@github.com:duckback00'
            GIT_DATICAL_REPO = 'epod_datical'
            GIT_SQL_REPO = 'epod_sql'
            PROJ_DDB = 'epod_datical'
            PROJ_SQL = 'epod_sql'
            DATICAL_PIPELINE = 'current'
            BRANCH = 'current'
            RELEASE_LABEL = "${BUILD_NUMBER}"
            ORACLE_HOME = '/opt/oracle/product/12.1/client'
            PATH = "$PATH:/home/delphix_os/DaticalDB/repl:$ORACLE_HOME/bin"
          }
          post {
            always {
              archiveArtifacts '**/daticaldb.log, **/Reports/**, **/Logs/**, **/Snapshots/** ,**/*.zip'
            }

          }
        }
