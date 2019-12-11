const PipelinesView= () => import('@/components/PipelinesView')
const PipelineView= () => import('@/components/PipelineView')
const NotFoundView= () => import('@/components/NotFoundView')

const routes = [
  {
    path: '/',
    component: PipelinesView
  },
  {
    path: '/:pipelineid/',
    component: PipelineView,
    props: true
  },
  {
    path: '/:pipelineid/:viewid/',
    component: PipelineView,
    props: true
  },
  {
    path: '*',
    component: NotFoundView
  }
]

export default routes
