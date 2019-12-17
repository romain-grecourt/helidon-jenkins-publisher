export default function (status, result) {
  if (status === 'RUNNING') {
    return 'Running'
  } else if (result === 'SUCCESS' || result === 'PASSED') {
    return 'Passed'
  } else if (result === 'FAILURE') {
    return 'Failed'
  } else if (result === 'UNSTABLE') {
    return 'Unstable'
  } else if (result === 'ABORTED') {
    return 'Aborted'
  } else {
    return 'Unknown'
  }
}
