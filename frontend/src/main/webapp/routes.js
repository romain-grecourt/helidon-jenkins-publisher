import Pipelines from '@/components/Pipelines'
const Pipeline = () => import('@/components/Pipeline')
const PipelineView = () => import('@/components/PipelineView')
const PipelineTests = () => import('@/components/PipelineTests')
const PipelineArtifacts = () => import('@/components/PipelineArtifacts')

const routes = [
  {
    path: '/',
    name: 'Pipelines',
    component: Pipelines 
  },
  {
    path: '/:id/',
    name: 'Pipeline',
    component: Pipeline,
    children:[
        {
          path: '',
          redirect: 'view'
        },
        {
          path: 'view',
          name: 'PipelineView',
          component: PipelineView
        },
        {
          path: 'tests',
          name: 'PipelineTests',
          component: PipelineTests
        },
        {
          path: 'artifacts',
          name: 'PipelineArtifacts',
          component: PipelineArtifacts
        }
    ]
  }
]

export default routes
