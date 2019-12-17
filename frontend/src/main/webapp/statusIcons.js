export default function (state, result) {
  if (state === 'RUNNING') {
    return 'mdi-cached'
  } else if (result === 'SUCCESS' || result === 'PASSED') {
    return 'mdi-checkbox-marked-circle'
  } else if (result === 'FAILURE' || result === 'FAILED') {
    return 'mdi-close-circle'
  } else if (result === 'UNSTABLE') {
    return 'mdi-alert-circle'
  } else if (result === 'ABORTED') {
    return 'mdi-minus-circle'
  } else if (result === 'SKIPPED') {
    return 'mdi-cancel'
  } else {
    return 'mdi-help-circle'
  }
}
