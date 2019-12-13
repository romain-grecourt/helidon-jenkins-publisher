package io.helidon.build.publisher.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import io.helidon.build.publisher.model.Stage;
import io.helidon.build.publisher.model.Step;

import com.cloudbees.groovy.cps.Block;
import com.cloudbees.groovy.cps.Continuation;
import com.cloudbees.groovy.cps.Envs;
import com.cloudbees.groovy.cps.impl.CpsCallableInvocation;
import com.cloudbees.groovy.cps.impl.CpsClosure;
import com.cloudbees.groovy.cps.impl.SourceLocation;
import com.cloudbees.groovy.cps.sandbox.DefaultInvoker;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import hudson.model.Actionable;
import hudson.model.Queue.Executable;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTBranch;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.Safepoint;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

/**
 * A cache of step signatures from the pipeline model used to detect steps that are declared in the pipeline script and filter
 * out the steps that are generated.
 * This allows to avoid publishing output that contains sensitive information to the outside world.
 */
final class PipelineSignatures {

    private static final Map<String, WeakReference<PipelineSignatures>> SIGNATURES_CACHE = new WeakHashMap<>();
    final List<String> signatures;

    private PipelineSignatures(List<String> signatures) {
        this.signatures = Collections.unmodifiableList(signatures);
    }

    /**
     * Get or create the pipeline signatures for the given execution.
     * @param execution the execution to get the step signatures of
     * @return PipelineSignatures
     */
    static PipelineSignatures getOrCreate(FlowExecution execution) {
        if (!(execution instanceof CpsFlowExecution)) {
            throw new IllegalArgumentException("Unsupported execution");
        }
        String pipelineScript = ((CpsFlowExecution) execution).getScript();
        synchronized(SIGNATURES_CACHE) {
            WeakReference<PipelineSignatures> signaturesRef = SIGNATURES_CACHE.get(pipelineScript);
            if (signaturesRef != null && signaturesRef.get() != null) {
                return signaturesRef.get();
            }
        }
        ModelASTStages stagesModel = getStagesModel(execution);
        GroovyShell shell = ((CpsFlowExecution) execution).getShell();
        PipelineSignatures signatures = new PipelineSignatures(createSignatures(stagesModel.getStages(), shell));
        synchronized(SIGNATURES_CACHE) {
            WeakReference<PipelineSignatures> signaturesRef = SIGNATURES_CACHE.get(pipelineScript);
            if (signaturesRef != null && signaturesRef.get() != null) {
                return signaturesRef.get();
            }
            SIGNATURES_CACHE.put(pipelineScript, new WeakReference<>(signatures));
            return signatures;
        }
    }

    /**
     * Test if this instance contains a signature matching the given step.
     * @param signature signature to test
     * @return {@code true} if there is a step matching, {@code false} otherwise
     */
    boolean contains(String signature) {
        return signatures.contains(signature);
    }

    /**
     * Get the AST stages model for the given execution.
     * @param execution execution from which to get the AST model
     * @return ModelASTStages
     * @throws IllegalStateException if an error occurs while getting the stages model
     */
    private static ModelASTStages getStagesModel(FlowExecution execution) {
        try {
            FlowExecutionOwner owner = execution.getOwner();
            Executable exec = owner.getExecutable();
            if (exec instanceof Actionable) {
                ExecutionModelAction ema = ((Actionable) owner.getExecutable()).getAction(ExecutionModelAction.class);
                return ema.getStages();
            }
            throw new IllegalStateException("Unable to get stages model, executable is not supported");
        } catch (IOException e) {
            throw new  IllegalStateException(e);
        }
    }

    /**
     * Create the signature for a leaf scripted step.
     * @param step the step
     * @param sig the signature prefix
     * @return the created signature
     */
    private static String createSignature(ScriptedStep step, String sig) {
        String args = "";
        if (step.args instanceof Map) {
            for (Object stepArg : ((Map) step.args).values()) {
                args += stepArg.toString();
            }
        } else if (step.args instanceof List) {
            for (Object stepArg : ((List) step.args)) {
                args += stepArg.toString();
            }
        } else if (step.args != null) {
            args = step.args.toString();
        }
        return Step.createPath(sig, step.method, args);
    }

    /**
     * Create the signature for the given scripted steps.
     * @param steps the steps to create the signatures of
     * @param sig the signature prefix
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ScriptedStep> steps, String sig) {
        List<String> sigs = new LinkedList<>();
        for(ScriptedStep step : steps) {
            if (step.isMeta) {
                continue;
            }
            if ("parallel".equals(step.method)) {
                // special case for parallel since it's a leaf
                // but it creates nested steps
                if (step.args instanceof Map) {
                    for (Entry<Object, Object> entry : ((Map<Object, Object>) step.args).entrySet()) {
                        Object arg = entry.getValue();
                        String branchName = entry.getKey().toString();
                        if (arg instanceof ScriptedStepsSequence) {
                            try {
                                sigs.addAll(createSignatures(((ScriptedStepsSequence) arg).steps,
                                        Stage.createPath(sig, true, URLEncoder.encode(branchName, "UTF-8"))));
                            } catch (UnsupportedEncodingException ex) {
                                throw new IllegalStateException(ex);
                            }
                        }
                    }
                }
            } else if (step.nested.isEmpty()) {
                // leaf
                sigs.add(createSignature(step, sig));
            } else {
                String ssig = Stage.createPath(sig, false, step.args.toString());
                List<Entry<String, ScriptedStep>> nsteps = new LinkedList<>();
                for(ScriptedStep nstep : step.nested) {
                    nsteps.add(new AbstractMap.SimpleEntry<>(ssig, nstep));
                }
                while(!nsteps.isEmpty()) {
                    List<Entry<String, ScriptedStep>> next = new LinkedList<>();
                    for(Entry<String, ScriptedStep> entry : nsteps) {
                        ScriptedStep nstep = entry.getValue();
                        String nsig = entry.getKey();
                        if (nstep.nested.isEmpty()) {
                            // leaf
                            sigs.add(createSignature(nstep, nsig));
                        } else {
                            String nextSig = Stage.createPath(nsig, false, nstep.args.toString());
                            for(ScriptedStep s : nstep.nested) {
                                next.add(new AbstractMap.SimpleEntry<>(nextSig, s));
                            }
                        }
                    }
                    nsteps = next;
                }
            }
        }
        return sigs;
    }

    /**
     * Create the signatures for the given AST steps.
     * @param msteps the model steps to create the signatures of
     * @param sig the signature prefix
     * @param shell the groovy shell to evaluate scripted steps
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ModelASTStep> msteps, String sig, GroovyShell shell) {
        List<String> sigs = new LinkedList<>();
        for (ModelASTStep mstep : msteps) {
            String sid = mstep.getName();
            Map<String, ?> args = mstep.getArgs().argListToMap();
            if ("script".equals(sid)) {
                String script = (String) args.get("scriptBlock");
                List<ScriptedStep> ssteps = eval(shell, script);
                sigs.addAll(createSignatures(ssteps, sig));
            } else {
                String stepArgs = "";
                for (Object arg : args.values()) {
                    stepArgs += arg.toString();
                }
                sigs.add(Step.createPath(sig, sid, stepArgs));
            }
        }
        return sigs;
    }

    /**
     * Create the signatures for the given AST stages.
     * @param mstages the stages to create the signature for
     * @param shell the groovy shell to evaluate scripted steps
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ModelASTStage> mstages, GroovyShell shell) {
        List<String> sigs = new LinkedList<>();
        List<Entry<String, ModelASTStage>> sstages = new LinkedList<>();
        String sig = "/";
        for(ModelASTStage mstage : mstages) {
            sstages.add(new AbstractMap.SimpleEntry<>(Stage.createPath(sig, false, mstage.getName()), mstage));
        }
        while(!sstages.isEmpty()) {
            List<Entry<String, ModelASTStage>> next = new LinkedList<>();
            for (Entry<String, ModelASTStage> entry : sstages) {
                ModelASTStage sstage = entry.getValue();
                String ssig = entry.getKey();
                for (ModelASTBranch b : sstage.getBranches()) {
                    String bpid = b.getName();
                    String bsig = ssig;
                    if (!"default".equals(bpid) && bpid != null) {
                        bsig += "/" + bpid;
                    }
                    sigs.addAll(createSignatures(b.getSteps(), bsig, shell));
                }
                if (sstage.getStages() != null) {
                    for(ModelASTStage nsstage : sstage.getStages().getStages()) {
                        String nssig = Stage.createPath(ssig, false, nsstage.getName());
                        next.add(new AbstractMap.SimpleEntry<>(nssig, nsstage));
                    }
                }
            }
            sstages = next;
        }
        return sigs;
    }

    /**
     * Model for a scripted step.
     */
    private static final class ScriptedStep {

        private final String method;
        private final Object args;
        private final List<ScriptedStep> nested;
        private final boolean isMeta;

        ScriptedStep(String method, Object args, List<ScriptedStep> nested, boolean meta) {
            this.method = method;
            this.args = args;
            this.nested = nested;
            this.isMeta = meta;
        }

        @Override
        public String toString() {
            return ScriptedStep.class.getSimpleName() + "{ "
                    + "method=" + method
                    + ", meta=" + isMeta
                    + ", args=" + args
                    + ", nested=" + nested
                    + " }";
        }
    }

    /**
     * Sequence of scripted steps.
     * I.e steps resolved as part of the same closure call
     */
    private static final class ScriptedStepsSequence {

        private final List<ScriptedStep> steps;

        ScriptedStepsSequence() {
            this.steps = Collections.emptyList();
        }

        ScriptedStepsSequence(List<ScriptedStep> steps) {
            this.steps = steps != null ? steps : Collections.emptyList();
        }

        @Override
        public String toString() {
            return ScriptedStepsSequence.class.getSimpleName() + "{ "
                    + steps.toString()
                    + " }";
        }
    }

    /**
     * Evaluate the steps from the given script text.
     * @param shell groovy shell
     * @param scriptText raw script text
     * @return list of top level steps
     */
    static List<ScriptedStep> eval(GroovyShell shell, String scriptText) {
        Script script = shell.parse(scriptText);
        try {
            script.run();
        } catch (CpsCallableInvocation e) {
            ScriptedStepInvoker invoker = new ScriptedStepInvoker();
            e.invoke(Envs.empty(invoker), SourceLocation.UNKNOWN, Continuation.HALT).run();
            return invoker.steps;
        }
        return Collections.emptyList();
    }

    /**
     * An CPS invoker that skips the steps method calls and instead evaluates all its argument to build a graph of
     * {@link ScriptedStep}.
     */
    private static final class ScriptedStepInvoker extends DefaultInvoker {

        private final transient List<ScriptedStep> steps = new LinkedList<>();
        private final transient Map<Object, List<ScriptedStep>> sequences = new HashMap<>();
        private volatile boolean resolving;

        @Override
        public Object methodCall(Object receiver, String method, Object[] args) throws Throwable {
            if (Safepoint.class.equals(receiver)) {
                // skip the safepoints
                return Block.NOOP;
            }
            boolean isStep = StepDescriptor.byFunctionName(method) != null;
            boolean isMetaStep = !StepDescriptor.metaStepsOf(method).isEmpty();
            if (!isStep && !isMetaStep) {
                // default invocation for non step method calls
                return super.methodCall(receiver, method, args);
            }

            if (!resolving) {
                // top level step
                try {
                    resolving = true;
                    steps.add(resolveStep(method, isMetaStep, args));
                } finally {
                    resolving = false;
                }
            } else {
                // nested step
                List<ScriptedStep> sequence = sequences.get(receiver);
                if (sequence != null) {
                    sequence.add(resolveStep(method, isMetaStep, args));
                }
            }
            return Block.NOOP;
        }

        /**
         * Invoke a closure and return the resolved sequence of steps.
         * @param closure closure to call
         * @return resolved steps
         */
        private ScriptedStepsSequence call(CpsClosure closure) {
            try {
                closure.call();
            } catch (CpsCallableInvocation inv) {
                sequences.put(closure, new LinkedList<>());
                inv.invoke(Envs.empty(this), SourceLocation.UNKNOWN, Continuation.HALT).run();
                return new ScriptedStepsSequence(sequences.remove(closure));
            }
            return new ScriptedStepsSequence();
        }

        /**
         * Resolve the given object by invoking callable(s).
         * @param o the initial object
         * @return resolved object
         */
        private Object resolve(Object o) {
            if (o instanceof Map) {
                Map<Object, Object> resolvedMap = new LinkedHashMap<>();
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) o).entrySet()) {
                    if (entry.getValue() instanceof CpsClosure) {
                        resolvedMap.put(entry.getKey(), call((CpsClosure) entry.getValue()));
                    } else {
                        resolvedMap.put(entry.getKey(), entry.getValue());
                    }
                }
                return resolvedMap;
            } else if (o instanceof List) {
                List<Object> resolvedList = new LinkedList<>();
                for (Object elt : (List) o) {
                    if (elt instanceof CpsClosure) {
                        resolvedList.add(call((CpsClosure) elt));
                    } else {
                        resolvedList.add(elt);
                    }
                }
                return resolvedList;
            } else if (o instanceof CpsClosure) {
                return call((CpsClosure) o);
            } else {
                // non callable
                return o;
            }
        }

        /**
         * Resolve the given method call arguments into a {@link ScriptedStep} instance.
         * @param args arguments containing callable and non callable objects
         * @return list of objects with the callable object replaced with their corresponding result
         */
        private ScriptedStep resolveStep(String method, boolean isMetaStep, Object... args) {
            Object resolveArgs = args.length > 0 ? resolve(args[0]) : null;
            List<ScriptedStep> nested = args.length > 1 && (args[1] instanceof CpsClosure)
                    ? call((CpsClosure) args[1]).steps
                    : Collections.emptyList();
            return new ScriptedStep(method, resolveArgs, nested, isMetaStep);
        }
    }
}
