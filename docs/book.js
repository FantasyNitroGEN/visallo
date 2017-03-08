module.exports = {
  title: 'Visallo',
  description: 'Visallo, a big data analysis and visualization platform to help analysts and investigators solve ambiguous problems.',
  gitbook: '3.2.x',
  language: 'en',
  direction: 'ltr',
  // UPDATE Makefile "plugins=" variable if changing
  plugins: [ '-sharing', 'ga', 'theme-visallo', 'github-embed' ],
  styles: {
      website: 'styles/website.css'
  },
  pluginsConfig: {
    ga: {
        token: 'UA-63006144-4'
    },
    lunr: {
        maxIndexSize: 1000000000
    },
    'theme-visallo': {
        canonicalBaseUrl: 'http://docs.visallo.org'
    }
  }
};
