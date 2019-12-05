import Pipelines from '@/components/Pipelines'
const Pipeline = () => import('@/components/Pipeline')
const PipelineView = () => import('@/components/PipelineView')
const TestsView = () => import('@/components/TestsView')
const ArtifactsView = () => import('@/components/ArtifactsView')
const NotFound = () => import('@/components/NotFound')

const routes = [
  {
    path: '/',
    component: Pipelines 
  },
  {
    path: '/:pipelineid/',
    component: Pipeline,
    children:[
        {
          path: '',
          redirect: 'view'
        },
        {
          path: 'view',
          component: PipelineView
        },
        {
          path: 'tests',
          component: TestsView
        },
        {
          path: 'artifacts',
          component: ArtifactsView
        }
    ]
  },
  {
    path: '/notfound',
    name: 'NotFound',
    component: NotFound
  },
  {
    path: '*',
    redirect: 'notfound'
  }
]

export default routes
