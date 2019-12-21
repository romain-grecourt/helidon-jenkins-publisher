export default function (status) {
  if (status === 'RUNNING') {
    return 'Running'
  } else if (status === 'SUCCESS' || status === 'PASSED') {
    return 'Passed'
  } else if (status === 'FAILURE') {
    return 'Failed'
  } else if (status === 'UNSTABLE') {
    return 'Unstable'
  } else if (status === 'ABORTED') {
    return 'Aborted'
  } else {
    return 'Unknown'
  }
}
