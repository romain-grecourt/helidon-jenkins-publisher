<template>
  <div class="output">
    <slot></slot>
  </div>
</template>
<style>
  .output {
    counter-reset: log;
    padding: 0;
    text-decoration: none;
    font: 13px "Source Code Pro", Menlo, Monaco, Consolas, "Courier New", monospace;
    display: block;
    position: relative;
    color: #E0E0E0;
  }
  .output > div.line {
    color: #eee;
    position: relative;
    display: block;
    padding: 0px 0 0 30px;
    line-height: 20px;
    word-break: break-all;
  }
  .output > div.line:before {
    counter-increment: log;
    content: counter(log);
    min-width: 25px;
    position: absolute;
    display: inline-block;
    text-align: right;
    margin-left: -35px;
    color: #777777;
  }
</style>
<script>
  export default {
    name: 'ConsoleOutput'
  }
</script>
