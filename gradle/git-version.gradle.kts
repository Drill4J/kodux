import java.io.*

if (version == Project.DEFAULT_VERSION && file(".git").exists()) {
    val tagPattern = "v[0-9]*.[0-9]*.[0-9]*"
    val abbrevRegex = "-(\\d+)-g([0-9a-f]+)$".toRegex()
    version = ByteArrayOutputStream().let { output ->
        exec {
            commandLine("git", "describe", "--tags", "--long", "--match", tagPattern)
            standardOutput = output
            isIgnoreExitValue = true
        }.takeIf { it.exitValue == 0 }
            ?.let { "$output".substring(1).trim() }
            ?.replace(abbrevRegex, "")
    } ?: version
}
