<template>
  <div
    v-if="dohtml"
    ref="container"
    class="output"
  >
    <slot />
  </div>
  <pre
    v-else
    ref="container"
    class="output"
  ><slot /></pre>
</template>
<style>
div.output {
  counter-reset: log;
  padding: 0;
  text-decoration: none;
  font: 13px "Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace;
  display: block;
  position: relative;
  color: #E0E0E0;
}
div..output > div.line {
  color: #eee;
  position: relative;
  display: block;
  padding: 0px 0 0 30px;
  line-height: 20px;
  word-break: break-all;
}
div.output > div.line:before {
  counter-increment: log;
  content: counter(log);
  min-width: 25px;
  position: absolute;
  display: inline-block;
  text-align: right;
  margin-left: -35px;
  color: #777777;
}
pre.output {
  white-space: pre-wrap;       /* Since CSS 2.1 */
  white-space: -moz-pre-wrap;  /* Mozilla, since 1999 */
  white-space: -pre-wrap;      /* Opera 4-6 */
  white-space: -o-pre-wrap;    /* Opera 7 */
  word-wrap: break-word;       /* Internet Explorer 5.5+ */
}
</style>
<script>
export default {
  name: 'ConsoleOutput',
  props: {
    dohtml: {
      type: Boolean,
      default: true
    }
  }
}
</script>
