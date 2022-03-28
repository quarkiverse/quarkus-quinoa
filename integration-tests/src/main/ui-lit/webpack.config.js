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
        })
    ]
};