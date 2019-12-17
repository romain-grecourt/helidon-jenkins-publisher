export default function (status, result) {
  if (status === 'RUNNING') {
    return 'blue'
  } else if (result === 'SUCCESS' || result === 'PASSED') {
    return 'green'
  } else if (result === 'FAILURE' || result === 'FAILED') {
    return 'red'
  } else if (result === 'UNSTABLE') {
    return 'orange'
  } else {
    return 'grey'
  }
}
