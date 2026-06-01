param location string = resourceGroup().location
param uniqueStringName string = uniqueString(resourceGroup().id)

param smtpHost string
param smtpPort string
param smtpUser string

@secure()
param smtpPassword string
param mailTo string

resource storageAccount 'Microsoft.Storage/storageAccounts@2022-09-01' = {
  name: 'stfeedbackbiiaprod'
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
      supportsHttpsTrafficOnly: true
      defaultToOAuthAuthentication: true
    }
}

resource tableServices 'Microsoft.Storage/storageAccounts/tableServices@2022-09-01' = {
  parent: storageAccount
  name: 'default'
}

resource feedbackTable 'Microsoft.Storage/storageAccounts/tableServices/tables@2022-09-01' = {
  parent: tableServices
  name: 'feedbacks'
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'appinsights-feedback-platform'
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    publicNetworkAccessForIngestion: 'Enabled'
    publicNetworkAccessForQuery: 'Enabled'
  }
}

resource hostingPlan 'Microsoft.Web/serverfarms@2022-03-01' = {
  name: 'plan-feedback-serverless'
  location: location
  sku: {
    name: 'Y1'
    tier: 'Dynamic'
  }
  properties: {}
}

resource functionApp 'Microsoft.Web/sites@2022-03-01' = {
  name: 'fnapp-feedback-platform'
  location: location
  kind: 'functionapp'
  properties: {
    serverFarmId: hostingPlan.id
    siteConfig: {
      appSettings: [
        {
          name: 'AzureWebJobsStorage'
          value: 'DefaultEndpointsProtocol=https;AccountName=${storageAccount.name};AccountKey=${storageAccount.listKeys().keys[0].value};EndpointSuffix=${environment().suffixes.storage}'
        }
        {
          name: 'WEBSITE_CONTENTAZUREFILECONNECTIONSTRING'
          value: 'DefaultEndpointsProtocol=https;AccountName=${storageAccount.name};AccountKey=${storageAccount.listKeys().keys[0].value};EndpointSuffix=${environment().suffixes.storage}'
        }
        {
          name: 'APPINSIGHTS_INSTRUMENTATIONKEY'
          value: appInsights.properties.InstrumentationKey
        }
        {
          name: 'FUNCTIONS_WORKER_RUNTIME'
          value: 'java'
        }
        {
          name: 'FUNCTIONS_EXTENSION_VERSION'
          value: '~4'
        }
        {
          name: 'SMTP_HOST'
          value: smtpHost
        }
        {
          name: 'SMTP_PORT'
          value: smtpPort
        }
        {
          name: 'SMTP_USER'
          value: smtpUser
        }
        {
          name: 'SMTP_PASSWORD'
          value: smtpPassword
        }
        {
          name: 'MAIL_TO'
          value: mailTo
        }
      ]
    }
  }
}