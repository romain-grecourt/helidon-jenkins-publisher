import Pipelines from '@/components/Pipelines'
const Pipeline = () => import('@/components/Pipeline')
const PipelineView = () => import('@/components/PipelineView')
const TestsView = () => import('@/components/TestsView')
const ArtifactsView = () => import('@/components/ArtifactsView')
const NotFound = () => import('@/components/NotFound')
const Error = () => import('@/components/Error')

const routes = [
  {
    path: '/',
    component: Pipelines 
  },
  {
    path: '/notfound',
    name: 'NotFound',
    component: NotFound,
    props: true
  },
  {
    path: '/error',
    name: 'Error',
    component: Error,
    props: true
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
    path: '*',
    redirect: '/notfound'
  }
]

export default routes
