export default function (status) {
  if (status === 'RUNNING') {
    return 'mdi-cached'
  } else if (status === 'SUCCESS' || status === 'PASSED') {
    return 'mdi-checkbox-marked-circle'
  } else if (status === 'FAILURE' || status === 'FAILED') {
    return 'mdi-close-circle'
  } else if (status === 'UNSTABLE') {
    return 'mdi-alert-circle'
  } else if (status === 'ABORTED') {
    return 'mdi-minus-circle'
  } else if (status === 'SKIPPED') {
    return 'mdi-cancel'
  } else {
    return 'mdi-help-circle'
  }
}
