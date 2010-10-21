def processIncludes = { file, vars->
  def buffer = "";
  file = new File(file)
  file.eachLine {
    if(it.contains("//@Include=")) {
      def name = it.substring(it.indexOf("//@Include=") + 11).trim();
      if(name.startsWith('$')) {
        buffer += ("\n" + vars[name.substring(1)])
      }
      else {
        buffer += ("\n" + new File(file.parent, name).text)
      }
    }
    else {
      buffer += ("\n" + it)
    }
  }
  return buffer
}

def base = args[0];

def plugin = processIncludes("$base/src/main/js/jquery.phono.js", [
  "phono-core" : processIncludes("$base/src/main/js/phono.js", null)
])

new File("$base/target/jquery.phono.js").text = plugin;