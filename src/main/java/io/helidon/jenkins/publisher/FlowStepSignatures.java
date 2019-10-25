package io.helidon.jenkins.publisher;

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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.ExecutionModelAction;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTBranch;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStages;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStep;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.cps.Safepoint;
import org.jenkinsci.plugins.workflow.cps.nodes.StepAtomNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

/**
 * A cache of step signatures from the pipeline model used to detect steps that are declared in the pipeline script and filter
 * out the steps that are generated.
 * This allows to avoid publishing output that contains sensitive information to the outside world.
 */
final class FlowStepSignatures {

    private static final Map<String, WeakReference<FlowStepSignatures>> SIGNATURES_CACHE = new WeakHashMap<>();
    private final List<String> signatures;

    private FlowStepSignatures(List<String> signatures) {
        this.signatures = Collections.unmodifiableList(signatures);
        for (String sig : signatures) {
            System.out.println("SIG: " + sig);
        }
    }

    /**
     * Get or create the flow signatures for the given flow execution.
     * @param execution the flow execution to get the step signatures of
     * @return FlowStepSignatures
     */
    static FlowStepSignatures getOrCreate(FlowExecution execution) {
        if (!(execution instanceof CpsFlowExecution)) {
            throw new IllegalArgumentException("Unsupported execution");
        }
        String pipelineScript = ((CpsFlowExecution) execution).getScript();
        synchronized(SIGNATURES_CACHE) {
            WeakReference<FlowStepSignatures> signaturesRef = SIGNATURES_CACHE.get(pipelineScript);
            if (signaturesRef != null && signaturesRef.get() != null) {
                return signaturesRef.get();
            }
        }
        ModelASTStages stagesModel = getStagesModel(execution);
        GroovyShell shell = ((CpsFlowExecution) execution).getShell();
        FlowStepSignatures signatures = new FlowStepSignatures(createSignatures(stagesModel.getStages(), shell));
        synchronized(SIGNATURES_CACHE) {
            WeakReference<FlowStepSignatures> signaturesRef = SIGNATURES_CACHE.get(pipelineScript);
            if (signaturesRef != null && signaturesRef.get() != null) {
                return signaturesRef.get();
            }
            SIGNATURES_CACHE.put(pipelineScript, new WeakReference<>(signatures));
            return signatures;
        }
    }

    /**
     * Test if this instance contains a signature matching the given step.
     * @param step step node to match the signature of
     * @return {@code true} if there is a step matching, {@code false} otherwise
     */
    boolean contains(StepAtomNode step) {
        return signatures.contains(createSignature(step));
    }

    /**
     * Get the AST stages model for the given flow execution.
     * @param execution flow execution
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
     * Create the signature for a given step node to match with the registered signatures.
     * @param step
     * @return the created signature
     */
    private static String createSignature(StepAtomNode step) {
        return "TODO";
    }

    /**
     * Create the signature for a leaf scripted step.
     * @param step the step
     * @param sigPrefix the signature prefix
     * @return the created signature
     */
    private static String createSignature(ScriptedStep step, String sigPrefix) {
        String stepSig = sigPrefix + "step(" + step.getMethod() + ")[";
        Object stepArgs = step.getArgs();
        if (stepArgs instanceof Map) {
            for (Object stepArg : ((Map) stepArgs).values()) {
                stepSig += Base64.getEncoder().encode(stepArg.toString().getBytes());
            }
        } else if (stepArgs instanceof List) {
            for (Object stepArg : ((List) stepArgs)) {
                stepSig += Base64.getEncoder().encode(stepArg.toString().getBytes());
            }
        } else if (stepArgs != null) {
            stepSig += Base64.getEncoder().encode(stepArgs.toString().getBytes());
        }
        return stepSig += "]";
    }

    /**
     * Create the signature for the given scripted steps.
     * @param steps the steps to create the signatures of
     * @param sigPrefix the signature prefix
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ScriptedStep> steps, String sigPrefix) {
        List<String> sigs = new LinkedList<>();
        for(ScriptedStep step : steps) {
            if (step.isMeta()) {
                continue;
            }
            if ("parallel".equals(step.getMethod())) {
                // special case for parallel since it's a leaf
                // but it creates nested steps
                if (step.getArgs() instanceof Map) {
                    for (Object stepArg : ((Map)step.getArgs()).values()) {
                        if (stepArg instanceof ScriptedStepsSequence) {
                            sigs.addAll(createSignatures(((ScriptedStepsSequence)stepArg).getSteps(), sigPrefix + "/parallel"));
                        }
                    }
                }
            } else if (step.getNested().isEmpty()) {
                // leaf
                sigs.add(createSignature(step, sigPrefix));
            } else {
                String stepSigPrefix = sigPrefix + "/" + step.getMethod() + "[" + step.getArgs() + "]";
                List<Entry<String, ScriptedStep>> nestedSteps = new LinkedList<>();
                for(ScriptedStep nestedStep : step.getNested()) {
                    nestedSteps.add(new AbstractMap.SimpleEntry<>(stepSigPrefix, nestedStep));
                }
                while(!nestedSteps.isEmpty()) {
                    nestedSteps = new LinkedList<>();
                    for(Entry<String, ScriptedStep> entry : nestedSteps) {
                        ScriptedStep nestedStep = entry.getValue();
                        String nestedStepSigPrefix = entry.getKey();
                        if (nestedStep.getNested().isEmpty()) {
                            // leaf
                            sigs.add(createSignature(nestedStep, nestedStepSigPrefix));
                        } else {
                            String nextSigPrefix = nestedStepSigPrefix + "/" + nestedStep.getMethod() + "[" + nestedStep.getArgs() + "]";
                            for(ScriptedStep stepGrandChild : nestedStep.getNested()) {
                                nestedSteps.add(new AbstractMap.SimpleEntry<>(nextSigPrefix, stepGrandChild));
                            }
                        }
                    }
                }
            }
        }
        return sigs;
    }

    /**
     * Create the signatures for the given AST steps.
     * @param steps the steps to create the signatures of
     * @param sigPrefix the signature prefix
     * @param shell the groovy shell to evaluate scripted steps
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ModelASTStep> steps, String sigPrefix, GroovyShell shell) {
        List<String> sigs = new LinkedList<>();
        for (ModelASTStep step : steps) {
            String stepName = step.getName();
            // skip meta step
            if (!StepDescriptor.metaStepsOf(stepName).isEmpty()) {
                continue;
            }
            Map<String, ?> stepArgs = step.getArgs().argListToMap();
            if ("script".equals(stepName)) {
                String scriptText = (String) stepArgs.get("scriptBlock");
                List<ScriptedStep> scriptSteps = ScriptedStep.eval(shell, scriptText);
                sigs.addAll(createSignatures(scriptSteps, sigPrefix));
            } else {
                String stepSig = sigPrefix + "/step(" + stepName + ")[";
                for (Object stepArg : stepArgs.values()) {
                    stepSig += Base64.getEncoder().encode(stepArg.toString().getBytes());
                }
                stepSig += "]";
                sigs.add(stepSig);
            }
        }
        return sigs;
    }

    /**
     * Create the signatures for the given AST stages.
     * @param stages the stages to create the signature for
     * @param shell the groovy shell to evaluate scripted steps
     * @return the created signatures
     */
    private static List<String> createSignatures(List<ModelASTStage> stages, GroovyShell shell) {
        List<String> sigs = new LinkedList<>();
        List<Entry<String, ModelASTStage>> nestedStages = new LinkedList<>();
        for(ModelASTStage stage : stages) {
            nestedStages.add(new AbstractMap.SimpleEntry<>("", stage));
        }
        while(!nestedStages.isEmpty()) {
            for (Entry<String, ModelASTStage> entry : nestedStages) {
                nestedStages = new LinkedList<>();
                ModelASTStage stage = entry.getValue();
                String stageSigPrefix = entry.getKey();
                for (ModelASTBranch stageBranch : stage.getBranches()) {
                    String branchName = stageBranch.getName();
                    createSignatures(stageBranch.getSteps(), stageSigPrefix + "/" + branchName != null ? branchName : "", shell);
                }
                String nextSigPrefix = stageSigPrefix + "/stage[" + stage.getName();
                for(ModelASTStage nestedStage : stage.getStages().getStages()) {
                    nestedStages.add(new AbstractMap.SimpleEntry<>(nextSigPrefix, nestedStage));
                }
            }
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
        private final boolean meta;

        ScriptedStep(String method, Object args, List<ScriptedStep> nested, boolean meta) {
            this.method = method;
            this.args = args;
            this.nested = nested;
            this.meta = meta;
        }

        String getMethod() {
            return method;
        }

        Object getArgs() {
            return args;
        }

        boolean isMeta() {
            return meta;
        }

        List<ScriptedStep> getNested() {
            return nested;
        }

        @Override
        public String toString() {
            return ScriptedStep.class.getSimpleName() + "{"
                    + "method=" + method
                    + ", meta=" + meta
                    + ", args=" + args
                    + ", nested=" + nested
                    + "}";
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
                return invoker.getSteps();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Sequence of steps.
     */
    private static final class ScriptedStepsSequence {

        private final List<ScriptedStep> steps;

        ScriptedStepsSequence() {
            this.steps = Collections.emptyList();
        }

        ScriptedStepsSequence(List<ScriptedStep> steps) {
            this.steps = steps != null ? steps : Collections.emptyList();
        }

        List<ScriptedStep> getSteps() {
            return steps;
        }

        @Override
        public String toString() {
            return ScriptedStepsSequence.class.getSimpleName() + "{"
                    + steps.toString()
                    + "}";
        }
    }

    /**
     * An CPS invoker that skips the steps method calls and instead evaluates all its argument to build a graph of
     * {@link ScriptedStep}.
     */
    private static final class ScriptedStepInvoker extends DefaultInvoker {

        private final transient List<ScriptedStep> steps = new LinkedList<>();
        private final transient Map<Object, List<ScriptedStep>> sequences = new HashMap<>();
        private volatile boolean resolving;

        /**
         * Get the top-level steps.
         * @return list of scripted steps
         */
        List<ScriptedStep> getSteps() {
            return steps;
        }

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
                    ? call((CpsClosure) args[1]).getSteps()
                    : Collections.emptyList();
            return new ScriptedStep(method, resolveArgs, nested, isMetaStep);
        }
    }
}
