export default function (status) {
  if (status === 'RUNNING') {
    return 'blue'
  } else if (status === 'SUCCESS' || status === 'PASSED') {
    return 'green'
  } else if (status === 'FAILURE' || status === 'FAILED') {
    return 'red'
  } else if (status === 'UNSTABLE') {
    return 'orange'
  } else {
    return 'grey'
  }
}
