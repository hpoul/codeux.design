const path = require('path');

module.exports = {
    entry: {
        main: ['./web/src/main.ts',
        './web/src/myprism.js']
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
        path: path.resolve(__dirname, 'web/assets/theme/script/')
    },
    mode: 'production',
};
