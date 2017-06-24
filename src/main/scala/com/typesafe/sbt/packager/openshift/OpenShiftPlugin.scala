package com.typesafe.sbt.packager.openshift

import com.typesafe.sbt.packager.Keys.maintainer
import com.typesafe.sbt.packager.docker.DockerPlugin._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.{dockerCommands, _}
import com.typesafe.sbt.packager.docker.{ExecCmd, _}
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.defaultLinuxInstallLocation
import sbt.AutoPlugin


object OpenShiftPlugin extends AutoPlugin {

  override def requires = DockerPlugin

  override def projectSettings = DockerPlugin.projectSettings ++ Seq(
    dockerBaseImage := "registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift",

    // todo;; k8s labels

    dockerCommands := {
      val dockerBaseDirectory = (defaultLinuxInstallLocation in Docker).value
      val user = "10001"
      val group = "0"

      val generalCommands = DockerPlugin.makeFrom(dockerBaseImage.value) +: DockerPlugin.makeMaintainer((maintainer in Docker).value).toSeq

      generalCommands ++
        Seq(makeUser("root"), makeWorkdir(dockerBaseDirectory), makeAdd(dockerBaseDirectory)) ++
        makeArbitraryUserAccessible("." :: Nil) ++
        dockerLabels.value.map(makeLabel) ++
        makeExposePorts(dockerExposedPorts.value, dockerExposedUdpPorts.value) ++
        makeVolumes(dockerExposedVolumes.value, user, group) ++
        Seq(makeUser(user), makeEntrypoint(dockerEntrypoint.value), makeCmd(dockerCmd.value))
    }
  )

  private[packager] final def makeArbitraryUserAccessible(directories: Seq[String]): Seq[CmdLike] = Seq(
    ExecCmd("RUN", Seq("chgrp", "-R", "0", ".") ++ directories: _*),
    ExecCmd("RUN", Seq("chmod", "-R", "g+rwX", ".") ++ directories: _*)
  )
}
