const path = require('path');
// Direct dependency of webpack 4, so always present alongside it.
const TerserPlugin = require('terser-webpack-plugin');

module.exports = {
    entry: {
        main: [
            './web/src/main.ts',
            './web/src/code-highlight.js'
        ]
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader?configFile=tsconfig.webpack.json',
                include: [path.resolve(__dirname, 'web/src')],
                exclude: [path.resolve(__dirname, 'node_modules'), path.resolve(__dirname, '_tools')]
            },
            {
                test: /\.css$/i,
                use: ['style-loader', 'css-loader'],
            }
        ]
    },
    output: {
        filename: 'main.js',
        path: path.resolve(__dirname, 'web/assets/theme/script/'),
        // webpack 4 defaults to md4, which OpenSSL 3 (Node 17+) refuses to provide.
        hashFunction: 'sha256'
    },
    optimization: {
        // terser-webpack-plugin 1.x hashes its cache keys with md4, which
        // OpenSSL 3 (Node 17+) refuses to provide. Turning the cache off
        // sidesteps it; this build is small enough not to miss it.
        minimizer: [new TerserPlugin({cache: false})]
    },
    mode: 'production',
};
