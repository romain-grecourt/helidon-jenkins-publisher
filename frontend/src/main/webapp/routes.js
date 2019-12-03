import Pipelines from '@/components/Pipelines'
const Pipeline = () => import('@/components/Pipeline')
const PipelineView = () => import('@/components/PipelineView')
const PipelineTests = () => import('@/components/PipelineTests')
const PipelineArtifacts = () => import('@/components/PipelineArtifacts')

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
          path: 'view/:stepid/',
          component: PipelineView
        },
        {
          path: 'tests',
          component: PipelineTests
        },
        {
          path: 'tests/:stageid/',
          component: PipelineTests
        },
        {
          path: 'artifacts',
          component: PipelineArtifacts
        },
        {
          path: 'artifacts/:stageid/',
          component: PipelineArtifacts
        }
    ]
  }
]

export default routes
