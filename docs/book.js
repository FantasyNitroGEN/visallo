module.exports = {
  title: 'Visallo',
  description: 'Visallo, a big data analysis and visualization platform to help analysts and investigators solve ambiguous problems.',
  gitbook: '3.x.x',
  language: 'en',
  direction: 'ltr',
  plugins: [ 'ga', 'theme-default' ],
  styles: {
      website: 'styles/website.css'
  },
  pluginsConfig: {
    ga: {
      token: 'UA-63006144-4'
    }
  },
  search: {
    maxIndexSize: 1000000000
  }
};
