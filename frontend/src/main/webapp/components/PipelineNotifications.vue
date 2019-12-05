<template>
  <v-list
    dense
    nav
  >
    <v-subheader>NOTIFICATIONS</v-subheader>
    <v-list-item-group>
      <template
        v-for="(item,i) in items"
      >
        <v-list-item
          :key="i"
          three-line
        >
          <v-list-item-icon>
            <v-icon
              :color="statusColors[item.status]"
            >
              {{ statusIcons[item.status] }}
            </v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-subtitle>{{ item.title }}</v-list-item-subtitle>
            <v-list-item-subtitle>
              <a
                :href="item.repositoryLink"
              >
                {{ item.repository }}
              </a>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-divider
          v-if="item < items.length"
          :key="i"
        />
      </template>
    </v-list-item-group>
  </v-list>
</template>
<script>
import statusColors from '@/statusColors'
import statusIcons from '@/statusIcons'
import statusText from '@/statusText'
export default {
  name: 'PipelineNotifications',
  data: () => ({
    statusColors: statusColors,
    statusIcons: statusIcons,
    statusText: statusText,
    items: [
      {
        title: 'Pull Request #1148: WIP: MP Reactive Messaging POC',
        repository: 'oracle/helidon',
        repositoryLink: 'https://github.com/oracle/helidon',
        status: 'SUCCESS'
      },
      {
        title: 'Pull Request #1148: WIP: MP Reactive Messaging POC',
        repository: 'oracle/helidon',
        repositoryLink: 'https://github.com/oracle/helidon',
        status: 'FAILURE'
      },
      {
        title: 'Pull Request #1148: WIP: MP Reactive Messaging POC',
        repository: 'oracle/helidon',
        repositoryLink: 'https://github.com/oracle/helidon',
        status: 'UNSTABLE'
      },
      {
        title: 'Pull Request #1148: WIP: MP Reactive Messaging POC',
        repository: 'oracle/helidon',
        repositoryLink: 'https://github.com/oracle/helidon',
        status: 'RUNNING'
      }
    ]
  })
}
</script>
