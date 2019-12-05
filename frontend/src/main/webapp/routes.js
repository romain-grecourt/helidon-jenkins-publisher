import PipelinesView from '@/components/PipelinesView'
import PipelineView from '@/components/PipelineView'
import NotFoundView from '@/components/NotFoundView'

const routes = [
  {
    path: '/',
    component: PipelinesView
  },
  {
    path: '/:pipelineid/',
    component: PipelineView,
    props: true,
  },
  {
    path: '/:pipelineid/:viewid/',
    component: PipelineView,
    props: true,
  },
  {
    path: '*',
    component: NotFoundView
  }
]

export default routes
