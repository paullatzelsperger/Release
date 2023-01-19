#!/bin/bash

declare -a components=("GradlePlugins" "Connector" "IdentityHub" "RegistrationService" "FederatedCatalog")

for component in "${components[@]}"
do
  if [ ! -z "$VERSION" ]
  then
    # replace the version into the gradle properties and settings, if they exist
    if [ -e $component/gradle.properties ]
    then
        sed -i "s/0.0.1-SNAPSHOT/$VERSION/g" $component/gradle.properties;
    fi
    if [ -e $component/settings.gradle.kts ]
    then
        sed -i "s/0.0.1-SNAPSHOT/$VERSION/g" $component/settings.gradle.kts;
    fi
  fi
  (cd $component; ./gradlew -Pskip.signing publishToMavenLocal)
done

cat << EOF > settings.gradle.kts
rootProject.name = "connector"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-SNAPSHOT")
            // this is not part of the published EDC Version Catalog, so we'll just "amend" it
            library("dnsOverHttps", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef("okhttp")
        }
    }
}

EOF

for component in "${components[@]}"
do
#  echo "include(\"$component\")" >> settings.gradle.kts
  cat $component/settings.gradle.kts | grep "include(" | grep -v "system-tests" | grep -v "client-cli" | grep -v "launcher" | sed --expression "s/\":/\":$component:/g" >> settings.gradle.kts

  sed -i "s/project(\":core/project(\":$component:core/g" $(find $component -name "build.gradle.kts")
  sed -i "s/project(\":data-protocols/project(\":$component:data-protocols/g" $(find $component -name "build.gradle.kts")
  sed -i "s/project(\":extensions/project(\":$component:extensions/g" $(find $component -name "build.gradle.kts")
  sed -i "s/project(\":launchers/project(\":$component:launchers/g" $(find $component -name "build.gradle.kts")
  sed -i "s/project(\":spi/project(\":$component:spi/g" $(find $component -name "build.gradle.kts")
  sed -i "s/project(\":system-tests/project(\":$component:system-tests/g" $(find $component -name "build.gradle.kts")
done
