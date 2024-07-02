const { EnvironmentPlugin } = require('webpack');
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
  entry: "./src/simple-greeting.js",
  output: {
    filename: "simple-greeting.js"
  },
  plugins: [
        new CopyWebpackPlugin({
            patterns: [
                { from: 'public' }
            ]
        }),
        new EnvironmentPlugin(['FOO', 'ROOT_PATH', 'API_PATH'])
    ]
};