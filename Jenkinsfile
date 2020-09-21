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

            stage('Environment') {
              steps {
                sh '''
			{ set +x; } 2>dev/null
											  
			###cd ${PROJ_DDB}
			PATH=/home/delphix_os/DaticalDB/repl:${PATH}
			
			RESULTS=`/opt/datical/dxtoolkit2/dx_get_env -engine delphix-vm-n-6 -name 172.16.129.133 --format json | jq ".results[] | length"`
			echo "Discover Results: ${RESULTS}"
			if [[ "${RESULTS}" == "" ]]
                        then			
                      		/opt/datical/dxtoolkit2/dx_create_env -d delphix-vm-n-6 -envname 172.16.129.133 -envtype unix -host 172.16.129.133 -username delphix -authtype password -password delphix -toolkitdir "/var/opt/delphix/toolkit"
			fi
                    '''
              }
            }

            stage('Ingest') {
              steps {
                sh '''
			{ set -x; } 2>/dev/null
											  
                   	#/opt/datical/dxtoolkit2/dx_ctl_dsource -d delphix-vm-n-6 -dever 5.3 -type oracle -sourcename orcl -sourceinst /u01/app/oracle/product/11.2.0.4/db_1 -sourceenv 172.16.129.133 -source_os_user delphix -dbuser delphixdb -password delphixdb -group Oracle_Source -dsourcename orcl -action create     
			###cd ${PROJ_DDB}
			PATH=/home/delphix_os/DaticalDB/repl:${PATH}
			
			RESULTS=`/opt/datical/dxtoolkit2/dx_get_db_env -engine delphix-vm-n-6 --format json | jq ".results[] | select(.Database == \\"orcl\\")"`
			echo "Ingest Results: ${RESULTS}"
			if [[ "${RESULTS}" == "" ]]
			then
				/opt/datical/dxtoolkit2/link_oracle_i.sh orcl Oracle_Source 172.16.129.133 orcl delphixdb delphixdb
			fi
                    '''
              }
            }

            stage('Provision') {
              parallel {
                stage('RefDB') {
                  steps {
                    sh '''
			{ set -x; } 2>/dev/null
											  
			###cd ${PROJ_DDB}
			PATH=/home/delphix_os/DaticalDB/repl:${PATH}
			RESULTS=`/opt/datical/dxtoolkit2/dx_get_db_env -engine delphix-vm-n-6 --format json | jq ".results[] | select(.Database == \\"orcl_ref\\")"`
			echo "Provision Results: ${RESULTS}"
			if [[ "${RESULTS}" == "" ]]
                        then
				/opt/datical/dxtoolkit2/dx_provision_vdb -engine delphix-vm-n-6 -type oracle -group Oracle_Targets -sourcename orcl -targetname orcl_ref -environment "172.16.129.133" -envinst "/u01/app/oracle/product/11.2.0.4/db_1" -template 200M -dbname orcl_ref -mntpoint /mnt/provision -autostart yes -configureclone "/opt/datical/dxtoolkit2/configureClone.sh"          
			fi
                     '''
                  }
                }

                stage('DevDB') {
                  steps {
                    sh '''
			{ set -x; } 2>/dev/null
											  
			###cd ${PROJ_DDB}
			PATH=/home/delphix_os/DaticalDB/repl:${PATH}
			RESULTS=`/opt/datical/dxtoolkit2/dx_get_db_env -engine delphix-vm-n-6 --format json | jq ".results[] | select(.Database == \\"VBITT\\")"`
			echo "Provision Results: ${RESULTS}"
			if [[ "${RESULTS}" == "" ]]
                        then
				/opt/datical/dxtoolkit2/dx_provision_vdb -engine delphix-vm-n-6 -type oracle -group Oracle_Targets -sourcename orcl -targetname VBITT -environment "172.16.129.133" -envinst "/u01/app/oracle/product/11.2.0.4/db_1" -template 200M -dbname VBITT -mntpoint /mnt/provision -autostart yes 
			fi   
                    '''
                  }
                }

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
			RESULTS=`/opt/datical/dxtoolkit2/dx_get_db_env -engine delphix-vm-n-6 --format json | jq ".results[] | select(.Database == \\"VBITT\\")"`
			echo "Dev Database Exist Results: ${RESULTS}"
			if [[ "${RESULTS}" != "" ]]
                        then
			        /opt/datical/dxtoolkit2/dx_snapshot_db -engine delphix-vm-n-6 -name VBITT 
			        hammer deploy dev --labels="${DATICAL_PIPELINE}"
				rc=$?
				if [[ ${rc} -ne 0 ]] 
				then
				    echo "err, rewinding VDB ..."
				    /opt/datical/dxtoolkit2/dx_rewind_db -engine delphix-vm-n-6 -name VBITT 
				else 
				    echo "packager code deployed successfully ..."
				fi	
			fi

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
