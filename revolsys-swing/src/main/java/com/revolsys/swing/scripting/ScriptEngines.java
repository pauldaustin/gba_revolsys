package com.revolsys.swing.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import com.revolsys.collection.LazyValueHolder;
import com.revolsys.value.ValueHolder;

public class ScriptEngines {

  private static final ValueHolder<ScriptEngine> JS = new LazyValueHolder<>(ScriptEngines::initJs);

  public static ScriptEngine getJs() {
    return JS.getValue();
  }

  public static ScriptEngine initJs() {
    try {
      final Engine engine = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false")
        .build();
      final Builder context = Context //
        .newBuilder("js")//
        .engine(engine)
        .allowHostAccess(HostAccess.ALL)//
        .allowHostClassLookup(s -> true)//
      ;
      return GraalJSScriptEngine.create(engine, context);
    } catch (final Exception e) {
      return new ScriptEngineManager().getEngineByName("nashorn");
    }
  }
}
