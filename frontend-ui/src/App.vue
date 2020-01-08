<template>
  <router-view />
</template>
<style>
@media screen and (min-width: 960px) {
  .px-5-md-and-up {
    padding-right: 20px !important;
    padding-left: 20px !important;
  }
}
@media screen and (max-width: 959px) {
  .px-1-sm-and-down {
    padding-right: 5px !important;
    padding-left: 5px !important;
  }
}
@media screen and (max-width: 959px) {
  .px-3-sm-and-down {
    padding-right: 12px !important;
    padding-left: 12px !important;
  }
}
@media screen and (max-width: 959px) {
  .mb-10-sm-and-down {
    margin-bottom: 80px;
  }
}
.truncate-ltr {
  -webkit-box-flex: 1;
  -ms-flex: 1 1 100%;
  flex: 1 1 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.truncate-rtl {
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  direction: rtl;
  text-align: left;
  line-height: 1.5;
}
.container.__fill-height {
  -webkit-box-align: center;
  -ms-flex-align: center;
  align-items: center;
   display: -webkit-box;
  display: -ms-flexbox;
  display: flex;
  -ms-flex-wrap: wrap;
  flex-wrap: wrap;
}
.__fill-height {
  height: 100%;
}
</style>